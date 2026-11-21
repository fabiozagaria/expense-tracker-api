package io.github.fabiozagaria.expensetracker.services;

import io.github.fabiozagaria.expensetracker.dto.MovementPage;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseCreateRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpensePatchRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseResponse;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseUpdateRequest;
import io.github.fabiozagaria.expensetracker.exceptions.NotFoundExpenseException;
import io.github.fabiozagaria.expensetracker.mapper.ExpenseMapper;
import io.github.fabiozagaria.expensetracker.model.expense.Expense;
import io.github.fabiozagaria.expensetracker.model.user.User;
import io.github.fabiozagaria.expensetracker.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import io.github.fabiozagaria.expensetracker.cache.OwnerExpenseCache;
import io.github.fabiozagaria.expensetracker.cache.OwnerExpensesChanged;
import io.github.fabiozagaria.expensetracker.dto.CappedList;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;


@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    private final EntityManager entityManager;
    private final OwnerExpenseCache cache;
    private final ApplicationEventPublisher events;



    @Transactional(readOnly = true)
    public MovementPage<ExpenseResponse> search(String username, String month, String query, int page, int size, String order) {
        var specification = MovementSearch.<Expense>owned(username, month, query);
        var pageable = MovementSearch.page(page, size, order);
        String key = "search:" + Base64.getUrlEncoder().encodeToString(month.getBytes(StandardCharsets.UTF_8))
                + ":" + Base64.getUrlEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8))
                + ":" + page + ":" + size + ":" + order;
        return cache.read(username, key, () -> MovementPage.from(
                expenseRepository.findAll(specification, pageable).map(this::toExpenseResponse)));
    }

    /**
     * Le spese più recenti, fino a CappedList.MAX_SIZE; il resto si legge con /search (paginato).
     * L'ordine restituito resta quello storico (id crescente), così per chi sta sotto il tetto
     * la risposta è identica a prima. La chiave "all:v2" evita di leggere dalla cache le vecchie
     * liste "all", che avevano un altro tipo.
     */
    @Transactional(readOnly = true)
    public CappedList<ExpenseResponse> findAllOwner(String username) {
        return cache.read(username, "all:v2", () -> {
            Slice<Expense> newest = expenseRepository.findByOwnerUsername(username,
                    PageRequest.of(0, CappedList.MAX_SIZE, Sort.by(Sort.Direction.DESC, "date", "id")));
            ArrayList<ExpenseResponse> items = newest.getContent().stream()
                    .map(this::toExpenseResponse)
                    .sorted(Comparator.comparing(ExpenseResponse::id))
                    .collect(Collectors.toCollection(ArrayList::new));
            return new CappedList<>(items, newest.hasNext());
        });
    }

    public Expense findByIdOwner(long id, String username) {
        return expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow( () -> new NotFoundExpenseException("Expense not found"));
    }

    @Transactional
    public Expense createExpense(ExpenseCreateRequest request, User user) {
        Expense newExpense = expenseMapper.toEntity(request, user);

        entityManager.persist(newExpense);
        events.publishEvent(new OwnerExpensesChanged(user.getUsername()));

        return newExpense;

    }
    

    @Transactional
    public Expense putExpenseById(long id, ExpenseUpdateRequest request, String username) {
        Expense expenseUpdate = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        expenseMapper.updateFromRequest(request, expenseUpdate);
        events.publishEvent(new OwnerExpensesChanged(username));
        return expenseUpdate;
    }

    @Transactional
    public Expense patchExpenseById(long id, ExpensePatchRequest request, String username) {
        Expense expenseUpdate = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        expenseMapper.patchFromRequest(request, expenseUpdate);

        events.publishEvent(new OwnerExpensesChanged(username));
        return expenseUpdate;
    }

    @Transactional
    public Expense deleteById(long id, String username) {
        Expense expense = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        entityManager.remove(expense);
        events.publishEvent(new OwnerExpensesChanged(username));
        return expense;
    }


    //MAPPER
    public ExpenseResponse toExpenseResponse(Expense expense) {
        return expenseMapper.toResponse(expense);
    }
}

package org.esercizi.expensetrackerapi.services;

import org.esercizi.expensetrackerapi.dto.MovementPage;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseResponse;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.exceptions.NotFoundExpenseException;
import org.esercizi.expensetrackerapi.mapper.ExpenseMapper;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.esercizi.expensetrackerapi.cache.OwnerExpenseCache;
import org.esercizi.expensetrackerapi.cache.OwnerExpensesChanged;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
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

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAllOwner(String username) {
        return cache.read(username, "all", () -> new ArrayList<>(expenseRepository
                .findAllByOwnerUsername(username).stream().map(this::toExpenseResponse).toList()));
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

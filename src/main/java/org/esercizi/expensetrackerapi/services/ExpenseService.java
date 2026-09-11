package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseResponse;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.exceptions.NotFoundExpenseException;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final EntityManager entityManager;



    public List<Expense> findAll() {
       return  expenseRepository.findAll();


    }

    public List<Expense> findAllOwner(String username) {
        return expenseRepository.findAllByIdAndOwnerUsername(username);
    }

    public Expense findByIdOwner(long id, String username) {
        return expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow( () -> new NotFoundExpenseException("Expense not found"));
    }

    @Transactional
    public Expense createExpense(ExpenseCreateRequest request, User user) {
        Expense newExpense = toExpense(request, user);

        entityManager.persist(newExpense);

        return newExpense;

    }
    

    @Transactional
    public Expense putExpenseById(long id, ExpenseUpdateRequest request, String username) {
        Expense expenseUpdate = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        expenseUpdate.setTitle(request.title());
        expenseUpdate.setAmount(request.amount());
        expenseUpdate.setCategory(request.category());
        expenseUpdate.setDate(request.date());
        expenseUpdate.setDescription(validDescription(request.description(), "description"));
        return expenseUpdate;
    }

    @Transactional
    public Expense patchExpenseById(long id, ExpensePatchRequest request, String username) {
        Expense expenseUpdate = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        if(request.title() != null) {
            expenseUpdate.setTitle(request.title().trim());
        }

        if(request.amount() != null) {
            expenseUpdate.setAmount(request.amount());
        }

        if(request.category() != null) {
            expenseUpdate.setCategory(request.category());
        }

        if(request.date() != null) {
            expenseUpdate.setDate(request.date());
        }

        if (request.description() != null) {
            String descriptionValid = validDescription(request.description(), "description");
            expenseUpdate.setDescription(descriptionValid);
        }

        return expenseUpdate;
    }

    @Transactional
    public Expense deleteById(long id, String username) {
        Expense expense = expenseRepository.findByIdAndOwnerUsername(id, username)
                .orElseThrow(() -> new NotFoundExpenseException("Expense not found"));

        entityManager.remove(expense);
        return expense;
    }


    //MAPPER
    public ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getDate()

        );
    }

    public Expense toExpense(ExpenseCreateRequest request, User user) {
        return new Expense(
                null,
                request.title(),
                request.amount(),
                request.description(),
                request.category(),
                request.date(),
                user

        );
    }

    private String validDescription(String value, String description) {
            //se null, assente
            if(value == null) {
                return null;
            }

        return value.trim();


    }
}

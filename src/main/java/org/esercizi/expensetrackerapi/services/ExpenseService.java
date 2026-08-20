package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import org.esercizi.expensetrackerapi.dto.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.ExpenseResponse;
import org.esercizi.expensetrackerapi.dto.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.exceptions.NotFoundExpenseException;
import org.esercizi.expensetrackerapi.model.Expense;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final EntityManager entityManager;

    public ExpenseService(ExpenseRepository expenseRepository, EntityManager entityManager) {

        this.expenseRepository = expenseRepository;
        this.entityManager = entityManager;
    }

    public List<Expense> findAll() {
       return  expenseRepository.findAll();


    }

    public Expense findById(long id) {
        return expenseRepository.findById(id)
                .orElseThrow( () -> new NotFoundExpenseException("Expense not found"));
    }

    @Transactional
    public Expense createExpense(ExpenseCreateRequest request) {
        Expense newExpense = toExpense(request);

        entityManager.persist(newExpense);

        return newExpense;

    }

    @Transactional
    public Expense putExpenseById(long id, ExpenseUpdateRequest request) {
        Expense updateExpense = findById(id);
        entityManager.persist(updateExpense);

        updateExpense.setTitle(request.title());
        updateExpense.setAmount(request.amount());
        updateExpense.setCategory(request.category());
        updateExpense.setDate(request.date());
        updateExpense.setDescription(validStringVariableIstance(request.description(), "description"));



        return updateExpense;


    }

    @Transactional
    public Expense patchExpenseById(long id, ExpensePatchRequest request) {
        Expense patchExpense = findById(id);
        entityManager.persist(patchExpense);

        if(request.title() != null) {
            patchExpense.setTitle(request.title().trim());
        }

        if(request.amount() != null) {
            patchExpense.setAmount(request.amount());
        }

        if(request.category() != null) {
            patchExpense.setCategory(request.category());
        }

        if(request.date() != null) {
            patchExpense.setDate(request.date());
        }

        if (request.description() != null) {
            String descriptionValid = validStringVariableIstance(request.description(), "description");
            patchExpense.setDescription(descriptionValid);
        }

        return patchExpense;
    }


    //MAPPER
    public ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDate()
        );
    }
    public Expense toExpense(ExpenseCreateRequest request) {
        return new Expense(
                null,
                request.title(),
                request.amount(),
                request.description(),
                request.category(),
                request.date()
        );
    }

    private String validStringVariableIstance(String value, String variableIstanceName) throws IllegalArgumentException {

            if (value.isBlank()) {
                throw new IllegalArgumentException(String.format("""
                        %s format not valid
                        """,
                        variableIstanceName));
            } else {
                return value.trim();
            }

    }
}

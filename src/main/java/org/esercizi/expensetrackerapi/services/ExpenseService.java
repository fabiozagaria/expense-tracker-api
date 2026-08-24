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
    public Expense putExpense(long id, ExpenseUpdateRequest request) {
       Expense expenseUpdate = entityManager.find(Expense.class, id);
       if(expenseUpdate == null)
           throw new NotFoundExpenseException("Expense not found. Update rollback");
        expenseUpdate.setTitle(request.title());
        expenseUpdate.setAmount(request.amount());
        expenseUpdate.setCategory(request.category());
        expenseUpdate.setDate(request.date());
        expenseUpdate.setDescription(validStringVariableIstance(request.description(), "description"));
        return expenseUpdate;
    }

    @Transactional
    public Expense patchExpenseById(long id, ExpensePatchRequest request) {
        Expense expenseUpdate = entityManager.find(Expense.class, id);
        if(expenseUpdate == null)
            throw new NotFoundExpenseException("Expense not found. Update rollback");

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
            String descriptionValid = validStringVariableIstance(request.description(), "description");
            expenseUpdate.setDescription(descriptionValid);
        }

        return expenseUpdate;
    }

    @Transactional
    public Expense deleteById(long id) {
        Expense expense = entityManager.find(Expense.class, id);

        if(expense == null) {
            throw new NotFoundExpenseException("Expense not found. Delete rollback");
        }

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
            //se null, assente
            if(value == null) {
                return null;
            }
            //se vuoto, no
            if (value.isBlank()) {
                throw new IllegalArgumentException(String.format("""
                        %s format not valid
                        """,
                        variableIstanceName));
            } else {
                //se con valore idoneo, ok
                return value.trim();
            }

    }
}

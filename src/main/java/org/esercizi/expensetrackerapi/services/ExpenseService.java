package org.esercizi.expensetrackerapi.services;

import org.esercizi.expensetrackerapi.dto.ExpenseResponse;
import org.esercizi.expensetrackerapi.model.Expense;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<Expense> findAll() {
       return  expenseRepository.findAll();


    }

    public ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDate()
        );
    }
}

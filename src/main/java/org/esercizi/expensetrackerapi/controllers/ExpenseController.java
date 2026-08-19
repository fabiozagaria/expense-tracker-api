package org.esercizi.expensetrackerapi.controllers;

import org.esercizi.expensetrackerapi.dto.ExpenseResponse;
import org.esercizi.expensetrackerapi.model.Expense;
import org.esercizi.expensetrackerapi.services.ExpenseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseResponse> getExpenses() {
        List<Expense> expenseList = expenseService.findAll();
        return expenseList.stream()
                .map(expenseService::toExpenseResponse)
                .toList();
    }
}

package org.esercizi.expensetrackerapi.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.esercizi.expensetrackerapi.dto.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.ExpenseResponse;
import org.esercizi.expensetrackerapi.dto.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.services.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseResponse> getAllExpenses() {
        List<Expense> expenseList = expenseService.findAll();
        return expenseList.stream()
                .map(expenseService::toExpenseResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ExpenseResponse getExpenseById(
            @PathVariable long id
    ) {
        Expense expense = expenseService.findById(id);
        return expenseService.toExpenseResponse(expense);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> save(
            @Valid @RequestBody ExpenseCreateRequest request,
            HttpServletRequest httpServletRequest
    ){
        Expense expense = expenseService.createExpense(request);
        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI()))
                .body(expenseService.toExpenseResponse(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateAll(
            @PathVariable long id,
            @Valid @RequestBody ExpenseUpdateRequest request
            ) {
        Expense expense = expenseService.putExpense(id, request);
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));

    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable long id,
            @Valid @RequestBody ExpensePatchRequest request
            ) {
        Expense expense = expenseService.patchExpenseById(id, request);
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExpenseResponse> delete(
            @PathVariable long id
    ) {
        Expense expense = expenseService.deleteById(id);
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));
    }
}

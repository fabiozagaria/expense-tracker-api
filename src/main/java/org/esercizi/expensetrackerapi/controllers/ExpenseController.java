package org.esercizi.expensetrackerapi.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpensePatchRequest;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseResponse;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseUpdateRequest;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.services.ExpenseService;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserService userService;


    @GetMapping
    public List<ExpenseResponse> getAllExpensesOwner(
            Authentication authentication
    ) {
        List<Expense> expenseList = expenseService.findAllOwner(authentication.getName());
        return expenseList.stream()
                .map(expenseService::toExpenseResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ExpenseResponse getExpenseById(
            @PathVariable long id,
            Authentication authentication
    ) {
        Expense expense = expenseService.findByIdOwner(id, authentication.getName());
        return expenseService.toExpenseResponse(expense);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> save(
            @Valid @RequestBody ExpenseCreateRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ){
        User user = userService.findByUsername(authentication.getName());
        Expense expense = expenseService.createExpense(request, user);
        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI()))
                .body(expenseService.toExpenseResponse(expense));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateAll(
            @PathVariable long id,
            @Valid @RequestBody ExpenseUpdateRequest request,
            Authentication authentication
            ) {
        Expense expense = expenseService.putExpenseById(id, request, authentication.getName());
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));

    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(
            @PathVariable long id,
            @Valid @RequestBody ExpensePatchRequest request,
            Authentication authentication
            ) {
        Expense expense = expenseService.patchExpenseById(id, request, authentication.getName());
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExpenseResponse> delete(
            @PathVariable long id,
            Authentication authentication
    ) {
        Expense expense = expenseService.deleteById(id, authentication.getName());
        return ResponseEntity
                .ok(expenseService.toExpenseResponse(expense));
    }
}

package io.github.fabiozagaria.expensetracker.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import io.github.fabiozagaria.expensetracker.dto.MovementPage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseCreateRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpensePatchRequest;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseResponse;
import io.github.fabiozagaria.expensetracker.dto.expense.ExpenseUpdateRequest;
import io.github.fabiozagaria.expensetracker.model.expense.Expense;
import io.github.fabiozagaria.expensetracker.model.user.User;
import io.github.fabiozagaria.expensetracker.services.ExpenseService;
import io.github.fabiozagaria.expensetracker.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Spese", description = "CRUD, ricerca paginata e lista delle spese del proprietario.")
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserService userService;


    @GetMapping("/search")
    public MovementPage<ExpenseResponse> search(
            @RequestParam(defaultValue = "") String month,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String order,
            Authentication authentication) {
        return expenseService.search(authentication.getName(), month, query, page, size, order);
    }

    /**
     * Array di spese, al massimo CappedList.MAX_SIZE (le più recenti). Se ce ne sono altre,
     * l'header X-Result-Truncated vale true: per leggerle tutte si usa /search, paginato.
     */
    @Operation(summary = "Elenco delle spese", description = "Al massimo 1000 elementi, i più recenti, come array JSON. Se ne esistono altri la risposta ha l'header `X-Result-Truncated: true`: per leggerli tutti usare `/search`, paginato.")
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAllExpensesOwner(Authentication authentication) {
        return expenseService.findAllOwner(authentication.getName()).toResponse();
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

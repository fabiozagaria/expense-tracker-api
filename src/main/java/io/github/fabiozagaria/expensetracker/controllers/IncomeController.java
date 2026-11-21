package io.github.fabiozagaria.expensetracker.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import io.github.fabiozagaria.expensetracker.dto.MovementPage;
import io.github.fabiozagaria.expensetracker.dto.income.IncomeUpdateRequest;
import io.github.fabiozagaria.expensetracker.dto.income.IncomePatchRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.github.fabiozagaria.expensetracker.dto.income.IncomeCreateRequest;
import io.github.fabiozagaria.expensetracker.dto.income.IncomeResponse;
import io.github.fabiozagaria.expensetracker.model.income.Income;
import io.github.fabiozagaria.expensetracker.model.user.User;
import io.github.fabiozagaria.expensetracker.services.IncomeService;
import io.github.fabiozagaria.expensetracker.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Entrate", description = "CRUD, ricerca paginata e lista delle entrate del proprietario.")
@RestController
@RequestMapping("/api/incomes")
@RequiredArgsConstructor
public class IncomeController {
    private final IncomeService incomeService;
    private final UserService userService;

    @GetMapping("/search")
    public MovementPage<IncomeResponse> search(
            @RequestParam(defaultValue = "") String month,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String order,
            Authentication authentication) {
        return incomeService.search(authentication.getName(), month, query, page, size, order);
    }

    /**
     * Array di entrate, al massimo CappedList.MAX_SIZE (le più recenti). Se ce ne sono altre,
     * l'header X-Result-Truncated vale true: per leggerle tutte si usa /search, paginato.
     */
    @Operation(summary = "Elenco delle entrate", description = "Al massimo 1000 elementi, i più recenti, come array JSON. Se ne esistono altri la risposta ha l'header `X-Result-Truncated: true`: per leggerli tutti usare `/search`, paginato.")
    @GetMapping
    public ResponseEntity<List<IncomeResponse>> getAll(Authentication authentication) {
        return incomeService.findAllOwner(authentication.getName()).toResponse();
    }

    @PostMapping
    public ResponseEntity<IncomeResponse> save(
            @Valid @RequestBody IncomeCreateRequest request,
            HttpServletRequest httpServletRequest,
            Authentication authentication
    ) {
        User user = userService.findByUsername(authentication.getName());
        Income income = incomeService.createIncome(request, user);
        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI() + "/" + income.getId()))
                .body(incomeService.toIncomeResponse(income));
    }
    @GetMapping("/{id}")
    public IncomeResponse getById(@PathVariable long id, Authentication authentication) {
        return incomeService.toIncomeResponse(incomeService.findByIdOwner(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    public IncomeResponse put(@PathVariable long id,
            @Valid @RequestBody IncomeUpdateRequest request,
            Authentication authentication) {
        return incomeService.toIncomeResponse(incomeService.putIncomeById(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}")
    public IncomeResponse patch(@PathVariable long id,
            @Valid @RequestBody IncomePatchRequest request,
            Authentication authentication) {
        return incomeService.toIncomeResponse(incomeService.patchIncomeById(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public IncomeResponse delete(@PathVariable long id, Authentication authentication) {
        return incomeService.toIncomeResponse(incomeService.deleteById(id, authentication.getName()));
    }
}

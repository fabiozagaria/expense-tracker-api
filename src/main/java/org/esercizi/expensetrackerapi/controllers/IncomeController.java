package org.esercizi.expensetrackerapi.controllers;

import org.esercizi.expensetrackerapi.dto.MovementPage;
import org.esercizi.expensetrackerapi.dto.income.IncomeUpdateRequest;
import org.esercizi.expensetrackerapi.dto.income.IncomePatchRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.income.IncomeCreateRequest;
import org.esercizi.expensetrackerapi.dto.income.IncomeResponse;
import org.esercizi.expensetrackerapi.model.income.Income;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.services.IncomeService;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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

    @GetMapping
    public List<IncomeResponse> getAll(Authentication authentication) {
        return incomeService.findAllOwner(authentication.getName())
                .stream()
                .map(incomeService::toIncomeResponse)
                .toList();
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

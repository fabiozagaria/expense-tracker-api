package org.esercizi.expensetrackerapi.controllers;

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
                .created(URI.create(httpServletRequest.getRequestURI()))
                .body(incomeService.toIncomeResponse(income));
    }
}

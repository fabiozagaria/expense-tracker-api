package org.esercizi.expensetrackerapi.controllers;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.dashboard.DashboardSummaryResponse;
import org.esercizi.expensetrackerapi.services.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(Authentication authentication) {
        return dashboardService.getSummary(authentication.getName());
    }
}

package io.github.fabiozagaria.expensetracker.services;

import lombok.RequiredArgsConstructor;
import io.github.fabiozagaria.expensetracker.dto.dashboard.DashboardSummaryResponse;
import io.github.fabiozagaria.expensetracker.repository.ExpenseRepository;
import io.github.fabiozagaria.expensetracker.repository.IncomeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    public DashboardSummaryResponse getSummary(String username) {
        BigDecimal totalIncome = incomeRepository.sumAmountByOwnerUsername(username);
        BigDecimal totalExpense = expenseRepository.sumAmountByOwnerUsername(username);
        return new DashboardSummaryResponse(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense)
        );
    }
}

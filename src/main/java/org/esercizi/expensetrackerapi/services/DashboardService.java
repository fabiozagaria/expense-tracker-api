package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.dashboard.DashboardSummaryResponse;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.esercizi.expensetrackerapi.repository.IncomeRepository;
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

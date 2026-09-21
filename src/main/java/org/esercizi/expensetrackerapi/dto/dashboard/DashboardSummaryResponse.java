package org.esercizi.expensetrackerapi.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance
) {}

package org.esercizi.expensetrackerapi.dto.income;

import org.esercizi.expensetrackerapi.model.income.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeResponse(
        Long id,
        String title,
        BigDecimal amount,
        String description,
        IncomeCategory category,
        LocalDate date
) {}

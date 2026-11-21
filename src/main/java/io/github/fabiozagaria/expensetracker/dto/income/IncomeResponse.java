package io.github.fabiozagaria.expensetracker.dto.income;

import io.github.fabiozagaria.expensetracker.model.income.IncomeCategory;

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

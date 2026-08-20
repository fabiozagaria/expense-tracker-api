package org.esercizi.expensetrackerapi.dto;

import org.esercizi.expensetrackerapi.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal amount,
        String description,
        ExpenseCategory category,
        LocalDate date
) {
}

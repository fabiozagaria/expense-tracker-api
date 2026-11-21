package io.github.fabiozagaria.expensetracker.dto.expense;

import io.github.fabiozagaria.expensetracker.model.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal amount,
        String description,
        ExpenseCategory category,
        LocalDate date

) implements java.io.Serializable {
}

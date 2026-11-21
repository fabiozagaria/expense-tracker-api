package io.github.fabiozagaria.expensetracker.dto.expense;

import jakarta.validation.constraints.*;
import io.github.fabiozagaria.expensetracker.model.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseUpdateRequest(
        @NotBlank
        @Size(min = 3, max = 15)
        String title,

        @NotNull
        @Positive
        BigDecimal amount,

        @Size(max = 30)
        String description,

        @NotNull
        ExpenseCategory category,

        @NotNull
        @PastOrPresent
        LocalDate date
) {
}

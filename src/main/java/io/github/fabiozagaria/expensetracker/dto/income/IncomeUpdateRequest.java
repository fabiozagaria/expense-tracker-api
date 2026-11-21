package io.github.fabiozagaria.expensetracker.dto.income;

import jakarta.validation.constraints.*;
import io.github.fabiozagaria.expensetracker.model.income.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeUpdateRequest(
        @NotBlank @Size(min = 3, max = 15) String title,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 30) String description,
        @NotNull IncomeCategory category,
        @NotNull @PastOrPresent LocalDate date
) {}

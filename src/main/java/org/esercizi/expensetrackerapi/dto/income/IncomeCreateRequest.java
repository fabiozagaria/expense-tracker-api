package org.esercizi.expensetrackerapi.dto.income;

import jakarta.validation.constraints.*;
import org.esercizi.expensetrackerapi.model.income.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeCreateRequest(
        @NotBlank @Size(min = 3, max = 15) String title,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 30) String description,
        @NotNull IncomeCategory category,
        @NotNull @PastOrPresent LocalDate date
) {}

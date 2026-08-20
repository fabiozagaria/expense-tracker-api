package org.esercizi.expensetrackerapi.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import org.esercizi.expensetrackerapi.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseUpdateRequest(
        @NotBlank
        @Size(min = 3, max = 15 )
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

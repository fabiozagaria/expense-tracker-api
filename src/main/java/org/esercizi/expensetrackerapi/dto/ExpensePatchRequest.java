package org.esercizi.expensetrackerapi.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import org.esercizi.expensetrackerapi.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpensePatchRequest(

        @Size(min = 3, max = 25 )
        String title,


        @Positive
        BigDecimal amount,

        @Size(min = 10, max = 50)
        String description,


        @Enumerated(EnumType.STRING)
        ExpenseCategory category,


        @PastOrPresent
        LocalDate date
) {
}

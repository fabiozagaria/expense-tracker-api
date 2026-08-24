package org.esercizi.expensetrackerapi.dto;

import jakarta.validation.constraints.*;
import org.esercizi.expensetrackerapi.model.expense.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpensePatchRequest(

        @Size(min = 3, max = 15 )
        String title,


        @Positive
        BigDecimal amount,

        @Size(max = 30)
        String description,



        ExpenseCategory category,


        @PastOrPresent
        LocalDate date
) {
}

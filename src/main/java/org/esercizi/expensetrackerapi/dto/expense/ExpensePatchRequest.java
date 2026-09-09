package org.esercizi.expensetrackerapi.dto.expense;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

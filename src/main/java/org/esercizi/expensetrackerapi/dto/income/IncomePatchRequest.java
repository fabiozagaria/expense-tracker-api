package org.esercizi.expensetrackerapi.dto.income;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.esercizi.expensetrackerapi.model.income.IncomeCategory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomePatchRequest(

        @Pattern(regexp = "(?s).*\\S.*")
        @Size(min = 3, max = 15 )
        String title,


        @Positive
        BigDecimal amount,

        @Size(max = 30)
        String description,



        IncomeCategory category,


        @PastOrPresent
        LocalDate date
) {
}

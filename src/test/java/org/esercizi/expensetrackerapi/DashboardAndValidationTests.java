package org.esercizi.expensetrackerapi;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.esercizi.expensetrackerapi.dto.expense.ExpenseCreateRequest;
import org.esercizi.expensetrackerapi.model.expense.ExpenseCategory;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.esercizi.expensetrackerapi.repository.IncomeRepository;
import org.esercizi.expensetrackerapi.services.DashboardService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAndValidationTests {
    final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dashboardBalanceIsIncomeMinusExpense() {
        var expenses = mock(ExpenseRepository.class);
        var incomes = mock(IncomeRepository.class);
        when(incomes.sumAmountByOwnerUsername("owner")).thenReturn(new BigDecimal("100.00"));
        when(expenses.sumAmountByOwnerUsername("owner")).thenReturn(new BigDecimal("130.25"));

        var summary = new DashboardService(expenses, incomes).getSummary("owner");

        assertThat(summary.totalIncome()).isEqualByComparingTo("100.00");
        assertThat(summary.totalExpense()).isEqualByComparingTo("130.25");
        assertThat(summary.balance()).isEqualByComparingTo("-30.25");
    }

    @Test
    void validExpenseRequestHasNoViolations() {
        var request = new ExpenseCreateRequest("Spesa", new BigDecimal("1.00"), "nota",
                ExpenseCategory.other, LocalDate.now());
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void invalidExpenseRequestIsRejectedOnEveryConstrainedField() {
        var request = new ExpenseCreateRequest("ab", new BigDecimal("-1"), "x".repeat(31),
                null, LocalDate.now().plusDays(1));
        var fields = validator.validate(request).stream()
                .map(v -> v.getPropertyPath().toString()).toList();
        assertThat(fields).containsExactlyInAnyOrder("title", "amount", "description", "category", "date");
    }
}

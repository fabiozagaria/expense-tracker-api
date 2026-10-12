package org.esercizi.expensetrackerapi.services;

import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.model.expense.ExpenseCategory;
import org.esercizi.expensetrackerapi.model.income.Income;
import org.esercizi.expensetrackerapi.model.income.IncomeCategory;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.repository.ExpenseRepository;
import org.esercizi.expensetrackerapi.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final IncomeRepository incomes;
    private final PasswordEncoder encoder;
    private final String password;

    public DemoDataInitializer(UserRepository users, ExpenseRepository expenses, IncomeRepository incomes,
                               PasswordEncoder encoder, @Value("${app.demo-password}") String password) {
        if (password.length() < 16) throw new IllegalArgumentException("DEMO_PASSWORD must have at least 16 characters");
        this.users = users;
        this.expenses = expenses;
        this.incomes = incomes;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var existing = users.findByUsername("portfolio-demo");
        if (existing.isPresent()) {
            User user = existing.get();
            if (!"portfolio-demo@example.invalid".equals(user.getEmail()) || user.getRole() != Role.USER) {
                throw new IllegalStateException("Demo username already belongs to another account");
            }
            user.setPassword(encoder.encode(password));
            user.setEmailVerified(true);
            return; // Never overwrite visitors' data on backend wake-up.
        }
        User user = users.save(User.builder().username("portfolio-demo")
                .email("portfolio-demo@example.invalid").password(encoder.encode(password))
                .emailVerified(true).role(Role.USER).build());
        LocalDate date = LocalDate.now().minusDays(1);
        expenses.save(Expense.builder().owner(user).title("Spesa alimenti")
                .amount(new BigDecimal("45.50")).category(ExpenseCategory.groceries)
                .description("Dati dimostrativi").date(date).build());
        expenses.save(Expense.builder().owner(user).title("Trasporti")
                .amount(new BigDecimal("20.00")).category(ExpenseCategory.transport)
                .description("Dati dimostrativi").date(date).build());
        incomes.save(Income.builder().owner(user).title("Stipendio demo")
                .amount(new BigDecimal("1500.00")).category(IncomeCategory.salary)
                .description("Dati dimostrativi").date(date).build());
    }
}

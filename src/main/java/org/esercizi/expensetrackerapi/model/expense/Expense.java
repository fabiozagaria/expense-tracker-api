package org.esercizi.expensetrackerapi.model.expense;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.esercizi.expensetrackerapi.model.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "expenses")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expenses")
    private Long id;

    @NotBlank
    @Size(min = 3, max = 15)
    private String title;

    @NotNull
    @Positive
    @Column(nullable = false)
    private BigDecimal amount;

    @Size(max = 30)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory category;

    @NotNull
    @PastOrPresent
    @Column(nullable = false)
    private LocalDate date;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

}
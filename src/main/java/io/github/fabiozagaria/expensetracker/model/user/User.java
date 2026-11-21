package io.github.fabiozagaria.expensetracker.model.user;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import io.github.fabiozagaria.expensetracker.model.EmailVerificationToken;
import io.github.fabiozagaria.expensetracker.model.expense.Expense;
import io.github.fabiozagaria.expensetracker.model.income.Income;
import io.github.fabiozagaria.expensetracker.security.refresh.RefreshToken;

import java.util.List;

@Entity
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    @Size(min = 6, max = 20)
    private String username;

    @NotNull
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false, unique = true)
    @Email
    private String email;

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @NotNull
    @Column(nullable = false)
    private boolean emailVerified;

    @OneToMany(mappedBy = "owner")
    private List<Expense> expenseList;

    @OneToMany(mappedBy = "owner")
    private List<Income> incomeList;

    @OneToMany(mappedBy = "user")
    private List<RefreshToken> refreshTokenList;

    @OneToMany(mappedBy = "user")
    private List<EmailVerificationToken> emailVerificationTokenList;
}

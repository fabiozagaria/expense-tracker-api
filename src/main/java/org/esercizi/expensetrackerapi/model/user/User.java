package org.esercizi.expensetrackerapi.model.user;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.esercizi.expensetrackerapi.model.expense.Expense;
import org.esercizi.expensetrackerapi.security.refresh.RefreshToken;

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

    @NotNull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;


    @OneToMany(mappedBy = "owner")
    private List<Expense> expenseList;

    @OneToMany(mappedBy = "user")
    private List<RefreshToken> refreshTokenList;
}

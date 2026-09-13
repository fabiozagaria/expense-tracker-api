package org.esercizi.expensetrackerapi.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.esercizi.expensetrackerapi.model.user.User;

import java.time.Instant;

@Entity
@Table(name = "verification_tokens")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false)
    private Instant createAt;

    @NotNull
    @Column(nullable = false)
    private Instant expireAt;

    @NotNull
    @Column(nullable = false)
    private Instant usedAt;
}

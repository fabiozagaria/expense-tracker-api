package org.esercizi.expensetrackerapi.security.refresh;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.esercizi.expensetrackerapi.model.user.User;

import java.time.Instant;

@Entity
@Table(name = "refresh")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
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
    private Instant createAt;

    @NotNull
    private Instant expireAt;

    private Instant revokeAt;

}

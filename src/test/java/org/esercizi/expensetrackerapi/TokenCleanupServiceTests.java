package org.esercizi.expensetrackerapi;

import org.esercizi.expensetrackerapi.model.EmailVerificationToken;
import org.esercizi.expensetrackerapi.model.PasswordResetToken;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.esercizi.expensetrackerapi.repository.PasswordResetTokenRepository;
import org.esercizi.expensetrackerapi.repository.RefreshTokenRepository;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.security.refresh.RefreshToken;
import org.esercizi.expensetrackerapi.services.TokenCleanupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Non transazionale: il job gira nella propria transazione e cancella davvero. */
@SpringBootTest
class TokenCleanupServiceTests {
    @Autowired TokenCleanupService cleanup;
    @Autowired UserRepository users;
    @Autowired RefreshTokenRepository refreshTokens;
    @Autowired EmailVerificationTokenRepository verificationTokens;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    private String username;

    @AfterEach
    void cleanUp() {
        String owner = "(select id from users where username = ?)";
        jdbc.update("delete from refresh where user_id = " + owner, username);
        jdbc.update("delete from verification_tokens where user_id = " + owner, username);
        jdbc.update("delete from password_reset_tokens where user_id = " + owner, username);
        jdbc.update("delete from users where username = ?", username);
    }

    @Test
    void deletesOnlyTokensExpiredBeyondTheRetentionPeriod() {
        username = "clean" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password("x").role(Role.USER).emailVerified(true).build());
        Instant now = Instant.now();
        Instant longExpired = now.minus(TokenCleanupService.RETENTION).minus(Duration.ofHours(1));
        Instant recentlyExpired = now.minus(Duration.ofHours(1));
        Instant active = now.plus(Duration.ofDays(1));

        refreshTokens.save(refresh(user, "r-old", longExpired, longExpired));
        refreshTokens.save(refresh(user, "r-recent", recentlyExpired, null));
        // Revocato ma ancora valido per data: serve alla reuse detection e NON va cancellato.
        refreshTokens.save(refresh(user, "r-revoked-active", active, now));
        refreshTokens.save(refresh(user, "r-active", active, null));
        verificationTokens.save(EmailVerificationToken.builder().tokenHash("v-old").user(user)
                .createAt(longExpired).expireAt(longExpired).build());
        verificationTokens.save(EmailVerificationToken.builder().tokenHash("v-active").user(user)
                .createAt(now).expireAt(active).build());
        resetTokens.save(PasswordResetToken.builder().tokenHash("p-old").user(user)
                .createAt(longExpired).expireAt(longExpired).build());
        resetTokens.save(PasswordResetToken.builder().tokenHash("p-active").user(user)
                .createAt(now).expireAt(active).build());

        cleanup.deleteExpiredTokens();

        assertThat(refreshTokens.findByTokenHash("r-old")).isEmpty();
        assertThat(refreshTokens.findByTokenHash("r-recent")).isPresent();
        assertThat(refreshTokens.findByTokenHash("r-revoked-active")).isPresent();
        assertThat(refreshTokens.findByTokenHash("r-active")).isPresent();
        assertThat(verificationTokens.findByTokenHash("v-old")).isEmpty();
        assertThat(verificationTokens.findByTokenHash("v-active")).isPresent();
        assertThat(resetTokens.findByTokenHash("p-old")).isEmpty();
        assertThat(resetTokens.findByTokenHash("p-active")).isPresent();
    }

    private static RefreshToken refresh(User user, String hash, Instant expireAt, Instant revokeAt) {
        return RefreshToken.builder().tokenHash(hash).user(user)
                .createAt(Instant.now().minus(Duration.ofDays(8))).expireAt(expireAt).revokeAt(revokeAt).build();
    }
}

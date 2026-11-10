package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import org.esercizi.expensetrackerapi.exceptions.InvalidPasswordResetTokenException;
import org.esercizi.expensetrackerapi.model.PasswordResetToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.PasswordResetTokenRepository;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.security.OpaqueTokens;
import org.esercizi.expensetrackerapi.security.refresh.RefreshTokenService;
import org.esercizi.expensetrackerapi.services.mail.VerificationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock PasswordResetTokenRepository tokens;
    @Mock UserRepository users;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock VerificationEmailSender emailSender;
    @Mock EntityManager entityManager;

    PasswordResetService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokens, users, refreshTokenService, passwordEncoder,
                emailSender, entityManager);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.example.test/");
        user = User.builder().username("someuser").email("some@example.test")
                .password("old-hash").emailVerified(true).build();
    }

    private PasswordResetToken tokenCreatedAgo(Duration age) {
        Instant created = Instant.now().minus(age);
        return PasswordResetToken.builder().id(1L).tokenHash("h").user(user)
                .createAt(created).expireAt(created.plus(PasswordResetService.TOKEN_TTL)).build();
    }

    @Test
    void unknownEmailDoesNothing() {
        when(users.findByEmail("nobody@example.test")).thenReturn(Optional.empty());

        service.requestReset("nobody@example.test");

        verifyNoInteractions(tokens, emailSender);
    }

    @Test
    void unverifiedUserDoesNothing() {
        user.setEmailVerified(false);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.requestReset(user.getEmail());

        verifyNoInteractions(tokens, emailSender);
    }

    @Test
    void requestWithinCooldownIsSilentlyIgnored() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserOrderByCreateAtDesc(user))
                .thenReturn(Optional.of(tokenCreatedAgo(Duration.ofSeconds(5))));

        service.requestReset(user.getEmail());

        verify(tokens, never()).deleteByUser(any());
        verifyNoInteractions(emailSender);
    }

    @Test
    void storesOnlyTheHashAndEmailsTheRawToken() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserOrderByCreateAtDesc(user)).thenReturn(Optional.empty());

        service.requestReset(user.getEmail());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).deleteByUser(user);
        verify(tokens).save(saved.capture());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(org.mockito.ArgumentMatchers.eq(user.getEmail()), anyString(),
                text.capture(), anyString());

        Matcher m = Pattern.compile("https://app\\.example\\.test/reset-password\\?token=([A-Za-z0-9_-]+)")
                .matcher(text.getValue());
        assertThat(m.find()).isTrue();
        String raw = m.group(1);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(OpaqueTokens.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpireAt())
                .isAfter(Instant.now().plus(Duration.ofMinutes(29)))
                .isBefore(Instant.now().plus(Duration.ofMinutes(31)));
    }

    @Test
    void resetWithUnknownTokenFails() {
        when(tokens.findByTokenHash(OpaqueTokens.hash("bad"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("bad", "new-password"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        verifyNoInteractions(passwordEncoder, refreshTokenService);
    }

    @Test
    void resetWithUsedOrExpiredTokenFailsAndKeepsPassword() {
        PasswordResetToken token = tokenCreatedAgo(Duration.ofMinutes(40));
        when(tokens.findByTokenHash(OpaqueTokens.hash("raw"))).thenReturn(Optional.of(token));
        when(tokens.consumeIfActive(anyLong(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.resetPassword("raw", "new-password"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);

        assertThat(user.getPassword()).isEqualTo("old-hash");
        verifyNoInteractions(passwordEncoder, refreshTokenService);
    }

    @Test
    void validResetChangesPasswordAndRevokesAllSessions() {
        PasswordResetToken token = tokenCreatedAgo(Duration.ofMinutes(1));
        when(tokens.findByTokenHash(OpaqueTokens.hash("raw"))).thenReturn(Optional.of(token));
        when(tokens.consumeIfActive(anyLong(), any())).thenReturn(1);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword("raw", "new-password");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(refreshTokenService).revokeAllFor(user);
    }
}

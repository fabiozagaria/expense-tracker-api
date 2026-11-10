package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import org.esercizi.expensetrackerapi.model.EmailVerificationToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.esercizi.expensetrackerapi.security.access.JwtService;
import org.esercizi.expensetrackerapi.security.refresh.RefreshTokenService;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceResendVerificationTests {

    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserService userService;
    @Mock EntityManager entityManager;
    @Mock RefreshTokenService refreshTokenService;
    @Mock EmailService emailService;
    @Mock EmailVerificationTokenRepository tokens;
    @Mock UserRepository users;

    AuthService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(jwtService, authenticationManager, passwordEncoder, userService,
                entityManager, refreshTokenService, emailService, tokens, users);
        user = User.builder().username("someuser").email("some@example.test").emailVerified(false).build();
    }

    private EmailVerificationToken tokenCreatedAgo(Duration age) {
        Instant created = Instant.now().minus(age);
        return EmailVerificationToken.builder()
                .tokenHash("old").user(user).createAt(created).expireAt(created.plus(Duration.ofMinutes(10))).build();
    }

    @Test
    void unknownEmailDoesNothing() throws Exception {
        when(users.findByEmail("nobody@example.test")).thenReturn(Optional.empty());

        service.resendVerification("nobody@example.test");

        verifyNoInteractions(emailService, tokens);
    }

    @Test
    void alreadyVerifiedUserDoesNothing() throws Exception {
        user.setEmailVerified(true);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        service.resendVerification(user.getEmail());

        verifyNoInteractions(emailService, tokens);
    }

    @Test
    void requestWithinCooldownIsSilentlyIgnored() throws Exception {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserOrderByCreateAtDesc(user))
                .thenReturn(Optional.of(tokenCreatedAgo(Duration.ofSeconds(10))));

        service.resendVerification(user.getEmail());

        verify(tokens, never()).deleteByUser(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void expiredTokenIsReplacedAndNewEmailSent() throws Exception {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserOrderByCreateAtDesc(user))
                .thenReturn(Optional.of(tokenCreatedAgo(Duration.ofMinutes(30))));
        when(emailService.createVerificationToken(user)).thenReturn("new-raw-token");

        service.resendVerification(user.getEmail());

        // Ordine importante: prima si sostituisce il token, poi si invia l'email irreversibile.
        InOrder order = inOrder(tokens, emailService, entityManager);
        order.verify(tokens).deleteByUser(user);
        order.verify(emailService).createVerificationToken(user);
        order.verify(entityManager).flush();
        order.verify(emailService).sendVerificationEmail(user, "new-raw-token");
    }

    @Test
    void userWithoutAnyTokenGetsOne() throws Exception {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findFirstByUserOrderByCreateAtDesc(user)).thenReturn(Optional.empty());
        when(emailService.createVerificationToken(user)).thenReturn("raw");

        service.resendVerification(user.getEmail());

        verify(emailService).sendVerificationEmail(user, "raw");
    }
}

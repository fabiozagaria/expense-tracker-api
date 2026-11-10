package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.exceptions.InvalidPasswordResetTokenException;
import org.esercizi.expensetrackerapi.model.PasswordResetToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.PasswordResetTokenRepository;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.security.OpaqueTokens;
import org.esercizi.expensetrackerapi.security.refresh.RefreshTokenService;
import org.esercizi.expensetrackerapi.services.mail.VerificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationEmailSender emailSender;
    private final EntityManager entityManager;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Invia il link di reimpostazione. Non rivela se l'email esiste: utente sconosciuto,
     * email non verificata e cooldown attivo escono tutti in silenzio.
     */
    @Transactional
    public void requestReset(String email) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !user.isEmailVerified()) {
            return;
        }
        boolean inCooldown = tokens.findFirstByUserOrderByCreateAtDesc(user)
                .map(last -> last.getCreateAt().plus(RESEND_COOLDOWN).isAfter(Instant.now()))
                .orElse(false);
        if (inCooldown) {
            return;
        }
        // Un solo link attivo per utente: una nuova richiesta invalida le precedenti.
        tokens.deleteByUser(user);

        String rawToken = OpaqueTokens.generate();
        Instant now = Instant.now();
        tokens.save(PasswordResetToken.builder()
                .tokenHash(OpaqueTokens.hash(rawToken))
                .user(user)
                .createAt(now)
                .expireAt(now.plus(TOKEN_TTL))
                .build());
        // Verifica i vincoli del database prima dell'invio, che è irreversibile.
        entityManager.flush();
        sendEmail(user, rawToken);
    }

    /** Imposta la nuova password e chiude tutte le sessioni esistenti dell'utente. */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokens.findByTokenHash(OpaqueTokens.hash(rawToken))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        if (tokens.consumeIfActive(token.getId(), Instant.now()) == 0) {
            throw new InvalidPasswordResetTokenException();
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAllFor(user);
    }

    private void sendEmail(User user, String rawToken) {
        String url = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken;
        String text = """
                Ciao %s,

                abbiamo ricevuto una richiesta di reimpostazione della password, il link è valido per 30 minuti:

                %s

                Se non l'hai richiesta, ignora questa email: la tua password non cambia.
                """.formatted(user.getUsername(), url);
        String html = """
                <!doctype html><html lang="it"><body>
                <h1>Reimposta la password</h1><p>Ciao %s,</p>
                <p>Il link è valido per 30 minuti.</p>
                <p><a href="%s">Scegli una nuova password</a></p>
                <p>Se non l'hai richiesta, ignora questa email: la tua password non cambia.</p>
                </body></html>
                """.formatted(HtmlUtils.htmlEscape(user.getUsername()), HtmlUtils.htmlEscape(url));
        emailSender.send(user.getEmail(), "Reimposta la tua password", text, html);
    }
}

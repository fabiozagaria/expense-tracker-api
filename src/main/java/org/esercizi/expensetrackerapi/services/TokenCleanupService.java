package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.esercizi.expensetrackerapi.repository.PasswordResetTokenRepository;
import org.esercizi.expensetrackerapi.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Elimina i token scaduti, che altrimenti si accumulerebbero per sempre.
 * Si cancellano solo i token già scaduti: quelli revocati ma ancora validi per data restano,
 * perché servono a riconoscere il riuso di un refresh token ruotato.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {
    /** Margine dopo la scadenza prima di cancellare (utile per diagnosticare "link scaduto"). */
    public static final Duration RETENTION = Duration.ofDays(1);

    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;

    @Scheduled(cron = "${app.token-cleanup.cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpiredTokens() {
        Instant cutoff = Instant.now().minus(RETENTION);
        int refresh = refreshTokens.deleteExpiredBefore(cutoff);
        int verification = verificationTokens.deleteExpiredBefore(cutoff);
        int reset = resetTokens.deleteExpiredBefore(cutoff);
        log.info("Pulizia token scaduti: refresh={}, verifica email={}, reset password={}",
                refresh, verification, reset);
    }
}

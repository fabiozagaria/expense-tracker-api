package org.esercizi.expensetrackerapi.security.refresh;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.RefreshTokenRepository;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    /** Entro questo tempo dalla rotazione, un riuso dello stesso token è considerato una gara, non un furto. */
    static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final EntityManager entityManager;


    public String generateRefresh() {
        byte[] bytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);

        String rawRefresh = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        return rawRefresh;

    }


    public String save(String rawRefresh, String username) throws NoSuchAlgorithmException {

        String tokenHash = hashToken(rawRefresh);

        User user = userService.findByUsername(username);
        Instant now = Instant.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .createAt(now)
                .expireAt(now.plus(7, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenHash;
    }

    public RefreshToken verifyRefresh(String refresh) throws NoSuchAlgorithmException {
        String tokenHash = hashToken(refresh);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Token not found"));
        Instant now = Instant.now();

        if (refreshToken.getRevokeAt() != null)
            throw new InvalidRefreshTokenException("Token revoked");

        if (refreshToken.getExpireAt().isBefore(now))
            throw new InvalidRefreshTokenException("Token expired");


        return refreshToken;


    }

    /**
     * Come {@link #verifyRefresh}, ma per la rotazione: il riuso di un token GIÀ ruotato oltre la
     * finestra di tolleranza è il segnale tipico di un token rubato (il ladro e il proprietario ne
     * hanno una copia ciascuno). In quel caso si chiudono tutte le sessioni dell'utente.
     * Dentro la finestra è invece il normale scenario "due richieste di refresh insieme" (due tab,
     * un retry di rete): si respinge soltanto quella in ritardo.
     * Il chiamante deve usare noRollbackFor, altrimenti l'eccezione annulla la revoca.
     */
    public RefreshToken verifyForRotation(String refresh) throws NoSuchAlgorithmException {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(refresh))
                .orElseThrow(() -> new InvalidRefreshTokenException("Token not found"));
        Instant now = Instant.now();

        if (refreshToken.getExpireAt().isBefore(now))
            throw new InvalidRefreshTokenException("Token expired");

        if (refreshToken.getRevokeAt() != null) {
            if (refreshToken.getRevokeAt().plus(REUSE_GRACE).isBefore(now)) {
                log.warn("Riuso di un refresh token già ruotato: sessioni chiuse per l'utente id={}",
                        refreshToken.getUser().getId());
                revokeAllFor(refreshToken.getUser());
            }
            // Stesso messaggio in entrambi i casi: chi attacca non deve capire se ha fatto scattare l'allarme.
            throw new InvalidRefreshTokenException("Token revoked");
        }
        return refreshToken;
    }

    /**
     * Consuma il token in modo atomico: se un'altra richiesta lo ha già revocato
     * (o è scaduto nel frattempo) lancia InvalidRefreshTokenException.
     * Va chiamato dentro la transazione che emette il nuovo token.
     */
    public void consume(RefreshToken refreshToken) {
        if (refreshTokenRepository.revokeIfActive(refreshToken.getId(), Instant.now()) == 0)
            throw new InvalidRefreshTokenException("Token revoked");
    }

    public void revokeAllFor(User user) {
        refreshTokenRepository.revokeAllActiveByUser(user, Instant.now());
    }

    private String hashToken(String rawRefresh) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = messageDigest.digest(
                rawRefresh.getBytes(StandardCharsets.UTF_8)
        );

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);
    }
}

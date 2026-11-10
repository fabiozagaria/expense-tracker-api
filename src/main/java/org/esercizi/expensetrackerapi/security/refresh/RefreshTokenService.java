package org.esercizi.expensetrackerapi.security.refresh;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.RefreshTokenRepository;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
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

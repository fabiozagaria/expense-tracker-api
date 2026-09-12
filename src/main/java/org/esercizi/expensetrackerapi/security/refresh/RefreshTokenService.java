package org.esercizi.expensetrackerapi.security.refresh;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.RefreshTokenRepository;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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
                .refreshToken(tokenHash)
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
                .orElseThrow();
        Instant now = Instant.now();

        if (refreshToken.getRevokeAt() != null)
            throw new InvalidRefreshTokenException("Token revoked");

        if (refreshToken.getExpireAt().isBefore(now))
            throw new InvalidRefreshTokenException("Token expired");


        return refreshToken;


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

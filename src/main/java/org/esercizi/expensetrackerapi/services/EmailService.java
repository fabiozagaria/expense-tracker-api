package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.model.EmailVerificationToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final UserService userService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public String generateVerificationToken(User user) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[32];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);

        String verificationToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = messageDigest.digest(bytes);

        String hashToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);

        Instant now = Instant.now();

        EmailVerificationToken emailVerificationToken = EmailVerificationToken.builder()
                .tokenHash(hashToken)
                .user(user)
                .createAt(now)
                .expireAt(now.plus(10, ChronoUnit.MINUTES))
                .usedAt(null)
                .build();
        emailVerificationTokenRepository.save(emailVerificationToken);

        return verificationToken;
    }




}

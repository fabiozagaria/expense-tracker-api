package org.esercizi.expensetrackerapi.services;

import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.exceptions.InvalidVerificationTokenException;
import org.esercizi.expensetrackerapi.model.EmailVerificationToken;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.EmailVerificationTokenRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
public class EmailService {
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final JavaMailSender javaMailSender;

    public String createVerificationToken(User user) throws NoSuchAlgorithmException {
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

    @Transactional
    public void verifyEmail(String rawToken) throws NoSuchAlgorithmException {
        String hashToken = hashTokenEmail(rawToken);
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByTokenHash(hashToken)
                .orElseThrow(() -> new InvalidVerificationTokenException("Token not found"));
        Instant now = Instant.now();
        if (emailVerificationToken.getUsedAt().isBefore(now) || emailVerificationToken.getExpireAt().isBefore(now)) {
            throw new InvalidVerificationTokenException("Token non idoneo");
        }
        User user = emailVerificationToken.getUser();
        user.setEmailVerified(true);
        emailVerificationToken.setUsedAt(now);


    }

    public void sendVerificationEmail(User user, String rawToken) {
        String verificationUrl =
                "http://localhost:4200/verify-email?token=" + rawToken;
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setFrom("noreply@expense-tracker.local");
        mailMessage.setSubject("Conferma il tuo account");

        mailMessage.setText("""
                Ciao %s,
                
                clicca sul link per verificare il tuo account:
                
                %s
                """.formatted(user.getUsername(), verificationUrl));

        javaMailSender.send(mailMessage);

    }

    private String hashTokenEmail(String rawToken) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hashBytes);
    }




}

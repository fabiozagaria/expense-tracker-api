package io.github.fabiozagaria.expensetracker.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import io.github.fabiozagaria.expensetracker.dto.login.AccessAndRefresh;
import io.github.fabiozagaria.expensetracker.dto.login.LoginRequest;
import io.github.fabiozagaria.expensetracker.dto.login.RegistrationResponse;
import io.github.fabiozagaria.expensetracker.dto.user.UserCreateRequest;
import io.github.fabiozagaria.expensetracker.exceptions.EmailNotVerifiedException;
import io.github.fabiozagaria.expensetracker.exceptions.InvalidRefreshTokenException;
import io.github.fabiozagaria.expensetracker.model.user.User;
import io.github.fabiozagaria.expensetracker.repository.EmailVerificationTokenRepository;
import io.github.fabiozagaria.expensetracker.repository.UserRepository;
import io.github.fabiozagaria.expensetracker.exceptions.RegistrationConflictException;
import io.github.fabiozagaria.expensetracker.security.access.JwtService;
import io.github.fabiozagaria.expensetracker.security.refresh.RefreshToken;
import io.github.fabiozagaria.expensetracker.security.refresh.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EntityManager entityManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public RegistrationResponse register(UserCreateRequest request) throws NoSuchAlgorithmException {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new RegistrationConflictException(RegistrationConflictException.Field.USERNAME);
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new RegistrationConflictException(RegistrationConflictException.Field.EMAIL);
        }
        String username = request.username();
        String pswHashed = passwordEncoder.encode(request.password());
        String email = request.email();

        User user = userService.create(username, pswHashed, email);
        entityManager.persist(user);

        String verificationToken = emailService.createVerificationToken(user);
        // Check database constraints before the irreversible external email request.
        entityManager.flush();
        emailService.sendVerificationEmail(user, verificationToken);

        return new RegistrationResponse("Account creato! Verifica Email");


    }

    /**
     * Reinvia il link di verifica. Non rivela se l'email esiste, è già verificata o è in cooldown:
     * in tutti questi casi esce senza errori e senza inviare nulla.
     */
    @Transactional
    public void resendVerification(String email) throws NoSuchAlgorithmException {
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isEmailVerified()) {
            return;
        }
        boolean inCooldown = emailVerificationTokenRepository.findFirstByUserOrderByCreateAtDesc(user)
                .map(last -> last.getCreateAt().plus(RESEND_COOLDOWN).isAfter(Instant.now()))
                .orElse(false);
        if (inCooldown) {
            return;
        }
        // Un solo token attivo per utente: i precedenti (scaduti o no) vengono sostituiti.
        emailVerificationTokenRepository.deleteByUser(user);
        String verificationToken = emailService.createVerificationToken(user);
        entityManager.flush();
        emailService.sendVerificationEmail(user, verificationToken);
    }

    public AccessAndRefresh login(LoginRequest loginRequest) throws NoSuchAlgorithmException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )

        );

        User user = userService.findByUsername(authentication.getName());

        if (!user.isEmailVerified()) {

            throw new EmailNotVerifiedException("Verifica email di nuovo");
        }

        String access = jwtService.getAccessTokenJWT(authentication.getName());
        String rawRefresh = refreshTokenService.generateRefresh();
        refreshTokenService.save(rawRefresh, authentication.getName());

        return new AccessAndRefresh(access, rawRefresh);


    }

    @Transactional
    public void logout(String rawRefresh) throws NoSuchAlgorithmException {
        if (rawRefresh == null) {
            throw new InvalidRefreshTokenException("Cookie not found");
        }
        RefreshToken refreshTokenEntity = refreshTokenService.verifyRefresh(rawRefresh);

        refreshTokenEntity.setRevokeAt(Instant.now());

    }

    // noRollbackFor: se viene rilevato il riuso di un token, la revoca di tutte le sessioni deve
    // restare anche se poi la richiesta fallisce con InvalidRefreshTokenException.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AccessAndRefresh refresh(String rawToken) throws NoSuchAlgorithmException {
        if (rawToken == null)
            throw new InvalidRefreshTokenException("Cookie not exists");

        RefreshToken validRefreshToken = refreshTokenService.verifyForRotation(rawToken);
        User user = validRefreshToken.getUser();
        String username = user.getUsername();

        refreshTokenService.consume(validRefreshToken);

        String newRefreshRaw = refreshTokenService.generateRefresh();
        refreshTokenService.save(newRefreshRaw, username);

        String access = jwtService.getAccessTokenJWT(username);

        return new AccessAndRefresh(access, newRefreshRaw);
    }






}


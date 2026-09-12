package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.login.AccessAndRefresh;
import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.dto.user.UserCreateRequest;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.security.access.JwtService;
import org.esercizi.expensetrackerapi.security.refresh.RefreshToken;
import org.esercizi.expensetrackerapi.security.refresh.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EntityManager entityManager;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AccessAndRefresh register(UserCreateRequest request) throws NoSuchAlgorithmException {
        String username = request.username();
        String pswHashed = passwordEncoder.encode(request.password());

        User user = userService.create(username, pswHashed);
        entityManager.persist(user);

        String access = jwtService.getAccessTokenJWT(username);
        String rawRefresh = refreshTokenService.generateRefresh();
        refreshTokenService.save(rawRefresh, username);


        return new AccessAndRefresh(access, rawRefresh);


    }

    public AccessAndRefresh login(LoginRequest loginRequest) throws NoSuchAlgorithmException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );
        String access = jwtService.getAccessTokenJWT(authentication.getName());
        String rawRefresh = refreshTokenService.generateRefresh();
        refreshTokenService.save(rawRefresh, authentication.getName());

        return new AccessAndRefresh(access, rawRefresh);


    }

    @Transactional
    public AccessAndRefresh refresh(String rawToken) throws NoSuchAlgorithmException {
        if (rawToken == null)
            throw new InvalidRefreshTokenException("Cookie not exists");

        RefreshToken validRefreshToken = refreshTokenService.verifyRefresh(rawToken);
        User user = validRefreshToken.getUser();
        String username = user.getUsername();

        validRefreshToken.setRevokeAt(Instant.now());

        String newRefreshRaw = refreshTokenService.generateRefresh();
        String hashToken = refreshTokenService.save(newRefreshRaw, username);

        String access = jwtService.getAccessTokenJWT(username);

        return new AccessAndRefresh(access, newRefreshRaw);
    }


}

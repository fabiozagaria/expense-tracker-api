package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.login.AuthResponse;
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
import java.util.HashMap;
import java.util.Map;

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
    public AuthResponse register(UserCreateRequest request) {
        String username = request.username();
        String pswHashed = passwordEncoder.encode(request.password());

        User user = userService.create(username, pswHashed);
        entityManager.persist(user);


        return getToken(username);


    }

    public Authentication login(LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );
    }

    @Transactional
    public Map<String, String> refresh(String rawToken) throws NoSuchAlgorithmException {
        if (rawToken == null)
            throw new InvalidRefreshTokenException("Cookie not exists");

        RefreshToken validRefreshToken = refreshTokenService.verifyRefresh(rawToken);
        User user = validRefreshToken.getUser();
        String username = user.getUsername();

        validRefreshToken.setRevokeAt(Instant.now());
        String newRefreshRaw = refreshTokenService.generateRefresh(username);
        validRefreshToken.setRefreshToken(newRefreshRaw);
        String access = jwtService.getAccessTokenJWT(username);


        Map<String, String> map = new HashMap<>();
        map.put(access, newRefreshRaw);

        return map;
    }

    public AuthResponse getToken(Authentication authentication) {
        String username = authentication.getName();
        String accessToken = jwtService.getAccessTokenJWT(username);

        return new AuthResponse(
                accessToken
        );
    }

    public AuthResponse getToken(String username) {
        String accessToken = jwtService.getAccessTokenJWT(username);
        return new AuthResponse(
                accessToken
        );
    }
}

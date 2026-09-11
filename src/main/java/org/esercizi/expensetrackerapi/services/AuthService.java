package org.esercizi.expensetrackerapi.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.login.AuthResponse;
import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.dto.user.UserCreateRequest;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EntityManager entityManager;

    @Transactional
    public AuthResponse register(UserCreateRequest request) {
        String username = request.username();
        String pswHashed = passwordEncoder.encode(request.password());

        User user = userService.create(username, pswHashed);
        entityManager.persist(user);
        entityManager.flush();

        return getTokens(username);


    }

    public Authentication login(LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );
    }

    public AuthResponse getTokens(Authentication authentication) {
        String username = authentication.getName();
        String accessToken = jwtService.getAccessTokenJWT(username);
        return new AuthResponse(
                accessToken
        );
    }

    public AuthResponse getTokens(String username) {
        String accessToken = jwtService.getAccessTokenJWT(username);
        return new AuthResponse(
                accessToken
        );
    }
}

package org.esercizi.expensetrackerapi.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.esercizi.expensetrackerapi.dto.login.AccessAndRefresh;
import org.esercizi.expensetrackerapi.dto.login.AuthResponse;
import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.dto.user.UserCreateRequest;
import org.esercizi.expensetrackerapi.security.access.JwtService;
import org.esercizi.expensetrackerapi.security.refresh.RefreshTokenService;
import org.esercizi.expensetrackerapi.services.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody UserCreateRequest createRequest,
            HttpServletRequest httpServletRequest
    ) {
        AuthResponse authResponse = authService.register(createRequest);
        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI()))
                .body(authResponse);


    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        Authentication authentication = authService.login(loginRequest);

        log.info("UTENTE {} AUTENTICATO", loginRequest.username());

        AuthResponse authResponse = authService.getToken(authentication);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authResponse);


    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) throws NoSuchAlgorithmException {

        AccessAndRefresh accessAndRefresh = authService.refresh(refreshToken);

        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", accessAndRefresh.refresh())
                .httpOnly(true)
                .path("/auth")
                .secure(false)
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build();
        AuthResponse authResponse = new AuthResponse(accessAndRefresh.access());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(authResponse);



    }


}

package org.esercizi.expensetrackerapi.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.esercizi.expensetrackerapi.dto.login.AccessAndRefresh;
import org.esercizi.expensetrackerapi.dto.login.AuthResponse;
import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.dto.login.RegistrationResponse;
import org.esercizi.expensetrackerapi.dto.user.UserCreateRequest;
import org.esercizi.expensetrackerapi.services.AuthService;
import org.esercizi.expensetrackerapi.services.EmailService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final EmailService emailService;
    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;
    @Value("${app.cookie-same-site:Strict}")
    private String cookieSameSite;
    @Value("${app.registration-enabled:true}")
    private boolean registrationEnabled;



    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody UserCreateRequest createRequest,
            HttpServletRequest httpServletRequest
    ) throws NoSuchAlgorithmException {
        if (!registrationEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Registrazione non disponibile nella demo");
        }
        RegistrationResponse registrationResponse = authService.register(createRequest);


        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI()))
                .body(registrationResponse);


    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam(required = false) String token
    ) throws NoSuchAlgorithmException {
        emailService.verifyEmail(token);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) throws NoSuchAlgorithmException {
        AccessAndRefresh accessAndRefresh = authService.login(loginRequest);

        log.info("UTENTE {} AUTENTICATO", loginRequest.username());

        ResponseCookie responseCookie = setCookie(accessAndRefresh.refresh());

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new AuthResponse(
                        accessAndRefresh.access()
                ));


    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) throws NoSuchAlgorithmException {
        authService.logout(refreshToken);

        ResponseCookie deleteCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();

    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken
    ) throws NoSuchAlgorithmException {

        AccessAndRefresh accessAndRefresh = authService.refresh(refreshToken);

        ResponseCookie responseCookie = setCookie(accessAndRefresh.refresh());

        AuthResponse authResponse = new AuthResponse(accessAndRefresh.access());
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(authResponse);



    }

    public ResponseCookie setCookie(String rawRefresh) {
        return ResponseCookie
                .from("refresh_token", rawRefresh)
                .httpOnly(true)
                .path("/auth")
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(Duration.ofDays(7))
                .build();
    }


}


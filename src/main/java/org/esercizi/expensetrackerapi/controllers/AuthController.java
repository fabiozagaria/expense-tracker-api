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
    ) throws NoSuchAlgorithmException {
        AccessAndRefresh accessAndRefresh = authService.register(createRequest);

        ResponseCookie responseCookie = setCookie(accessAndRefresh.refresh());

        return ResponseEntity
                .created(URI.create(httpServletRequest.getRequestURI()))
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(new AuthResponse(
                        accessAndRefresh.access()
                ));


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
                .secure(false)
                .sameSite("Strict")
                .maxAge(Duration.ofDays(7))
                .build();
    }


}

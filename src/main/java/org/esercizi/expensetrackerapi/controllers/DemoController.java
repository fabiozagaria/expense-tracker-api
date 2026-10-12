package org.esercizi.expensetrackerapi.controllers;

import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.NoSuchAlgorithmException;

@RestController
@Profile("demo")
public class DemoController {
    private final AuthController authController;
    private final String password;

    public DemoController(AuthController authController, @Value("${app.demo-password}") String password) {
        this.authController = authController;
        this.password = password;
    }

    @PostMapping("/auth/demo")
    public ResponseEntity<?> login() throws NoSuchAlgorithmException {
        return authController.login(new LoginRequest("portfolio-demo", password));
    }
}

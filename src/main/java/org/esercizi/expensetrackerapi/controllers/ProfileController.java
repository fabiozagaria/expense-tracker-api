package org.esercizi.expensetrackerapi.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.esercizi.expensetrackerapi.dto.user.ProfileResponse;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profilo", description = "Dati dell'utente autenticato.")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class ProfileController {
    private final UserService userService;

    @GetMapping
    public ProfileResponse getProfile(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return new ProfileResponse(user.getUsername(), user.getEmail());
    }
}

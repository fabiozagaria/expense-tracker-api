package org.esercizi.expensetrackerapi.dto.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResendVerificationRequest(
        @NotBlank
        @Email
        @Size(max = 50)
        String email
) {
}

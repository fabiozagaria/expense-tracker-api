package org.esercizi.expensetrackerapi.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        @Size(min = 6, max = 12)
        String password
) {
}

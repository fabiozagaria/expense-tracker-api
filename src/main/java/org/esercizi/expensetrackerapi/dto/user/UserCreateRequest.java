package org.esercizi.expensetrackerapi.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 20, min = 6)
        String username,

        @NotBlank
        @Size(min = 6, max = 12)
        String password
) {
}

package org.esercizi.expensetrackerapi.dto.login;

import jakarta.validation.constraints.NotBlank;
import org.esercizi.expensetrackerapi.validation.MaxUtf8Bytes;

public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        @MaxUtf8Bytes(72)
        String password
) {
}

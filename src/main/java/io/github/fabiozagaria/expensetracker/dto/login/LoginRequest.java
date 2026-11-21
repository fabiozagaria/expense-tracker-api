package io.github.fabiozagaria.expensetracker.dto.login;

import jakarta.validation.constraints.NotBlank;
import io.github.fabiozagaria.expensetracker.validation.MaxUtf8Bytes;

public record LoginRequest(
        @NotBlank
        String username,

        @NotBlank
        @MaxUtf8Bytes(72)
        String password
) {
}

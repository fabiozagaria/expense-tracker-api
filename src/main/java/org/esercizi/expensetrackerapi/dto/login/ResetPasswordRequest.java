package org.esercizi.expensetrackerapi.dto.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.esercizi.expensetrackerapi.validation.MaxUtf8Bytes;

public record ResetPasswordRequest(
        @NotBlank
        @Size(max = 100)
        String token,

        @NotBlank
        @Size(min = 6)
        @MaxUtf8Bytes(72)
        String newPassword
) {
}

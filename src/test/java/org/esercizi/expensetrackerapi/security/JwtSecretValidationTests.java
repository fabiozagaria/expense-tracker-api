package org.esercizi.expensetrackerapi.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidationTests {

    @Test
    void rejectsMissingOrBlankSecret() {
        assertThatThrownBy(() -> SecurityConfig.requireStrongSecret(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecurityConfig.requireStrongSecret(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsSecretShorterThan32BytesWithoutLeakingIt() {
        String shortSecret = "x".repeat(31);

        assertThatThrownBy(() -> SecurityConfig.requireStrongSecret(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32")
                .hasMessageNotContaining(shortSecret);
    }

    @Test
    void countsBytesNotCharacters() {
        // 16 caratteri da 2 byte in UTF-8 = 32 byte
        assertThat(SecurityConfig.requireStrongSecret("è".repeat(16))).hasSize(32);
    }

    @Test
    void acceptsSecretOf32BytesOrMore() {
        assertThat(SecurityConfig.requireStrongSecret("x".repeat(32))).hasSize(32);
        assertThat(SecurityConfig.requireStrongSecret("x".repeat(64))).hasSize(64);
    }
}

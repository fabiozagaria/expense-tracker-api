package org.esercizi.expensetrackerapi.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxUtf8BytesValidatorTests {

    private static MaxUtf8BytesValidator validator(int max) {
        MaxUtf8BytesValidator v = new MaxUtf8BytesValidator();
        MaxUtf8Bytes annotation = new MaxUtf8Bytes() {
            @Override public int value() { return max; }
            @Override public String message() { return ""; }
            @Override public Class<?>[] groups() { return new Class<?>[0]; }
            @Override @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() { return new Class[0]; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return MaxUtf8Bytes.class; }
        };
        v.initialize(annotation);
        return v;
    }

    @Test
    void acceptsNullAndAsciiAtLimit() {
        MaxUtf8BytesValidator v = validator(72);
        assertThat(v.isValid(null, null)).isTrue();
        assertThat(v.isValid("a".repeat(72), null)).isTrue();
        assertThat(v.isValid("a".repeat(73), null)).isFalse();
    }

    @Test
    void countsBytesNotCharacters() {
        MaxUtf8BytesValidator v = validator(72);
        // 36 caratteri da 2 byte = 72 byte: ok; 37 = 74 byte: troppo
        assertThat(v.isValid("è".repeat(36), null)).isTrue();
        assertThat(v.isValid("è".repeat(37), null)).isFalse();
    }
}

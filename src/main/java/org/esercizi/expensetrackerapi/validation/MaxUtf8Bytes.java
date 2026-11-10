package org.esercizi.expensetrackerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * La stringa, codificata in UTF-8, non deve superare {@code value} byte.
 * Serve per le password: BCrypt considera solo i primi 72 BYTE, e un carattere
 * come "è" o un emoji ne occupa 2-4, quindi {@code @Size} (che conta i caratteri) non basta.
 */
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "must be at most {value} bytes when encoded as UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

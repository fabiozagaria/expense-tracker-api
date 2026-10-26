package org.esercizi.expensetrackerapi.services.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.mail")
public class VerificationMailProperties {
    public enum Provider { SMTP, BREVO, DISABLED }

    @NotNull
    private Provider provider = Provider.SMTP;
    @NotBlank @Email
    private String from = "noreply@expense-tracker.local";
    @NotBlank
    private String fromName = "Gestionale Spese";
    // No toString(): never expose this key in diagnostics.
    private String brevoApiKey = "";
    @Min(100) @Max(30000)
    private int connectTimeoutMs = 5000;
    @Min(100) @Max(30000)
    private int readTimeoutMs = 10000;
}

package org.esercizi.expensetrackerapi.email;

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
@ConfigurationProperties("app.email")
public class EmailProperties {
    public enum Provider { SMTP, BREVO, DISABLED }

    @NotNull
    private Provider provider = Provider.SMTP;
    @Email
    private String from = "";
    @NotBlank
    private String fromName = "Expense Tracker";
    // No toString(): the API key must never appear in diagnostic output.
    private String brevoApiKey = "";
    @Min(100) @Max(30000)
    private int connectTimeoutMs = 5000;
    @Min(100) @Max(30000)
    private int readTimeoutMs = 10000;
}

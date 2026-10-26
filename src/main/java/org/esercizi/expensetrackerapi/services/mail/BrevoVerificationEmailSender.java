package org.esercizi.expensetrackerapi.services.mail;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import java.net.http.HttpClient;

public class BrevoVerificationEmailSender implements VerificationEmailSender {
    static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private final RestClient client;
    private final String from;
    private final String fromName;

    public BrevoVerificationEmailSender(VerificationMailProperties properties) {
        this(builder(properties), properties.getBrevoApiKey(), properties.getFrom(), properties.getFromName());
    }

    BrevoVerificationEmailSender(RestClient.Builder builder, String apiKey, String from, String fromName) {
        Assert.hasText(apiKey, "BREVO_API_KEY is required for Brevo");
        Assert.hasText(from, "MAIL_FROM is required for Brevo");
        Assert.isTrue(!from.endsWith(".local"), "MAIL_FROM must be a verified Brevo sender");
        this.from = from;
        this.fromName = fromName;
        this.client = builder.baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
    }

    private static RestClient.Builder builder(VerificationMailProperties properties) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public void send(String recipient, String subject, String text, String html) {
        try {
            var response = client.post().uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("email", from, "name", fromName),
                            "to", List.of(Map.of("email", recipient)),
                            "subject", subject,
                            "textContent", text,
                            "htmlContent", html))
                    .retrieve().toEntity(EmailResponse.class);
            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null
                    || response.getBody().messageId() == null || response.getBody().messageId().isBlank()) {
                throw new EmailDeliveryException();
            }
        } catch (RestClientException exception) {
            // Do not expose provider responses, API keys or verification links.
            throw new EmailDeliveryException();
        }
    }

    public record EmailResponse(String messageId) {}
}


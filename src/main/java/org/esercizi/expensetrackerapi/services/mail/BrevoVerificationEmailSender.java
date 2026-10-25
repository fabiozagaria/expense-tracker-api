package org.esercizi.expensetrackerapi.services.mail;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;

@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoVerificationEmailSender implements VerificationEmailSender {
    private final RestClient client;
    private final String from;

    @org.springframework.beans.factory.annotation.Autowired
    public BrevoVerificationEmailSender(
            @Value("${app.mail.brevo-api-key}") String apiKey,
            @Value("${app.mail.from}") String from) {
        this(builder(), apiKey, from);
    }

    BrevoVerificationEmailSender(RestClient.Builder builder, String apiKey, String from) {
        Assert.hasText(apiKey, "BREVO_API_KEY is required for Brevo");
        Assert.hasText(from, "MAIL_FROM is required for Brevo");
        Assert.isTrue(!from.endsWith(".local"), "MAIL_FROM must be a verified Brevo sender");
        this.from = from;
        this.client = builder.baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
    }

    private static RestClient.Builder builder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public void send(String recipient, String subject, String text) {
        try {
            client.post().uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("email", from, "name", "Gestionale Spese"),
                            "to", List.of(Map.of("email", recipient)),
                            "subject", subject,
                            "textContent", text))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            // Do not expose provider responses, API keys or verification links.
            throw new EmailDeliveryException();
        }
    }
}


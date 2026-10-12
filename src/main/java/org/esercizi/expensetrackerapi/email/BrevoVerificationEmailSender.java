package org.esercizi.expensetrackerapi.email;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

public class BrevoVerificationEmailSender implements VerificationEmailSender {
    static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";
    private final RestClient client;
    private final EmailProperties properties;

    public BrevoVerificationEmailSender(RestClient client, EmailProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void send(String recipient, String subject, String textContent, String htmlContent) {
        var payload = new EmailRequest(new Sender(properties.getFrom(), properties.getFromName()),
                List.of(new Recipient(recipient)), subject, textContent, htmlContent);
        try {
            // A single HTTPS POST. Never automatically retry a potentially accepted email.
            var response = client.post().uri(ENDPOINT)
                    .header("api-key", properties.getBrevoApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve().toEntity(EmailResponse.class);
            if (response.getStatusCode() != HttpStatus.CREATED || response.getBody() == null
                    || response.getBody().messageId() == null || response.getBody().messageId().isBlank()) {
                throw new EmailDeliveryException();
            }
        } catch (RestClientException ignored) {
            // Provider bodies can contain addresses or secrets; do not expose them or attach a cause.
            throw new EmailDeliveryException();
        }
    }

    public record Sender(String email, String name) {}
    public record Recipient(String email) {}
    public record EmailRequest(Sender sender, List<Recipient> to, String subject,
                               String textContent, String htmlContent) {}
    public record EmailResponse(String messageId) {}
}

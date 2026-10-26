package org.esercizi.expensetrackerapi.services.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoVerificationEmailSenderTests {
    @Test
    void sendsJsonWithApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var sender = new BrevoVerificationEmailSender(builder, "test-key", "sender@example.com", "Gestionale Spese");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {"sender":{"email":"sender@example.com","name":"Gestionale Spese"},
                     "to":[{"email":"recipient@example.com"}],
                     "subject":"Conferma","textContent":"Link: https://example.com/?token=test","htmlContent":"<p>Link</p>"}
                    """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"test-message\"}"));
        sender.send("recipient@example.com", "Conferma", "Link: https://example.com/?token=test", "<p>Link</p>");
        server.verify();
    }

    @Test
    void providerErrorsDoNotExposeResponseOrCredentials() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var sender = new BrevoVerificationEmailSender(builder, "test-key", "sender@example.com", "Gestionale Spese");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("sensitive provider response"));
        var error = assertThrows(EmailDeliveryException.class,
                () -> sender.send("recipient@example.com", "Conferma", "token", "<p>Link</p>"));
        assertEquals("Invio email non riuscito. Riprova più tardi.", error.getMessage());
        assertNull(error.getCause());
        server.verify();
    }

    @Test
    void rejectsMissingCredentialsAndLocalSender() {
        assertThrows(IllegalArgumentException.class,
                () -> new BrevoVerificationEmailSender(RestClient.builder(), "", "sender@example.com", "Gestionale Spese"));
        assertThrows(IllegalArgumentException.class,
                () -> new BrevoVerificationEmailSender(RestClient.builder(), "key", "noreply@expense-tracker.local", "Gestionale Spese"));
    }
}

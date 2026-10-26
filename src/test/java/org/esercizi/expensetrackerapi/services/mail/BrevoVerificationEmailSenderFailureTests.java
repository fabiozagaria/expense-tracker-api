package org.esercizi.expensetrackerapi.services.mail;

import org.esercizi.expensetrackerapi.exceptions.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoVerificationEmailSenderFailureTests {
    MockRestServiceServer server;
    BrevoVerificationEmailSender sender;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        var properties = new VerificationMailProperties();
        properties.setFrom("sender@example.test");
        properties.setFromName("Expense Tracker");
        properties.setBrevoApiKey("synthetic-test-key");
        sender = new BrevoVerificationEmailSender(builder, properties.getBrevoApiKey(), properties.getFrom(), properties.getFromName());
    }

    @Test
    void sendsOneHttpsRequestWithVerifiedSenderAndBothContentFormats() {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "synthetic-test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value("sender@example.test"))
                .andExpect(jsonPath("$.sender.name").value("Expense Tracker"))
                .andExpect(jsonPath("$.to[0].email").value("recipient@example.test"))
                .andExpect(jsonPath("$.subject").value("Conferma il tuo account"))
                .andExpect(jsonPath("$.textContent").value("verification-link"))
                .andExpect(jsonPath("$.htmlContent").value("<p>verification-link</p>"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"provider-test-id\"}"));
        send();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 429, 500, 503})
    void rejectionIsSanitizedAndNeverRetried(int status) {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"synthetic-test-key recipient@example.test verification-link\"}"));
        assertSafeFailure();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"messageId\":\"\"}", "not-json"})
    void successRequiresAValidProviderAcknowledgement(String body) {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(body));
        assertSafeFailure();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 202, 204})
    void rejectsUnexpectedSuccessStatus(int status) {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"provider-test-id\"}"));
        assertSafeFailure();
        server.verify();
    }

    @Test
    void rejectsCreatedWithoutAcknowledgementBody() {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withStatus(HttpStatus.CREATED));
        assertSafeFailure();
        server.verify();
    }

    @Test
    void networkFailureIsNotRetried() {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withException(new IOException("synthetic-test-key")));
        assertSafeFailure();
        server.verify();
    }

    @Test
    void rejectsRedirectResponseWithoutSendingToAnotherEndpoint() {
        server.expect(requestTo(BrevoVerificationEmailSender.ENDPOINT))
                .andRespond(withStatus(HttpStatus.TEMPORARY_REDIRECT).header("Location", "https://other.example"));
        assertSafeFailure();
        server.verify();
    }

    private void send() {
        sender.send("recipient@example.test", "Conferma il tuo account", "verification-link", "<p>verification-link</p>");
    }

    private void assertSafeFailure() {
        assertThatThrownBy(this::send).isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Invio email non riuscito. Riprova più tardi.")
                .hasMessageNotContaining("synthetic-test-key")
                .hasMessageNotContaining("recipient@example.test")
                .hasMessageNotContaining("verification-link")
                .hasNoCause();
    }
}

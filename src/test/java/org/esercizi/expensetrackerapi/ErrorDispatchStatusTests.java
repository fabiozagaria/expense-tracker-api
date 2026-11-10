package org.esercizi.expensetrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Su un server HTTP reale: gli errori inviati con response.sendError (come il 403 del controllo
 * d'origine) passano da un dispatch interno verso /error. Se quel percorso richiede autenticazione, lo stato
 * diventa 401 e nasconde la causa. MockMvc non fa quel dispatch, quindi qui serve un server vero.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.demo-password=test-only-demo-password-1234",
        "spring.datasource.hikari.data-source-properties.sslMode=DISABLED"
})
@ActiveProfiles("demo")
class ErrorDispatchStatusTests {
    @LocalServerPort int port;
    @MockitoBean JavaMailSender mailSender;

    private RestClient http() {
        return RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { }).build();
    }

    @Test
    void anAuthRequestFromAnUnknownOriginIsReportedAs403NotMaskedAs401() {
        int status = http().post().uri("/auth/demo").header("Origin", "https://evil.example")
                .retrieve().toBodilessEntity().getStatusCode().value();

        assertThat(status).isEqualTo(403);
    }

    @Test
    void anAuthRequestWithoutOriginIsReportedAs403() {
        assertThat(http().post().uri("/auth/demo").retrieve().toBodilessEntity().getStatusCode().value())
                .isEqualTo(403);
    }

    @Test
    void theErrorEndpointIsNotReachableDirectlyWithoutAuthentication() {
        // Il dispatch ERROR è permesso, una richiesta diretta a /error no.
        assertThat(http().get().uri("/error").retrieve().toBodilessEntity().getStatusCode().value())
                .isEqualTo(401);
    }

    @Test
    void protectedEndpointsStillAnswer401WithoutAToken() {
        assertThat(http().get().uri("/api/expenses").retrieve().toBodilessEntity().getStatusCode().value())
                .isEqualTo(401);
    }
}

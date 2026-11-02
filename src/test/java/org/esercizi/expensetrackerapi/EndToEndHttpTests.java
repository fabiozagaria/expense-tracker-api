package org.esercizi.expensetrackerapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * E2E su server HTTP reale (porta casuale, commit veri sul database): registrazione, verifica email,
 * login, creazione spesa, dashboard e refresh. Non usa MockMvc né transazioni di test, quindi i dati
 * creati vengono rimossi in {@link #cleanup()}: usare solo un database di test dedicato.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EndToEndHttpTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    final String username = "e2e" + UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        String owner = "(select id from users where username = ?)";
        for (String table : new String[]{"expenses", "incomes", "refresh", "verification_tokens"}) {
            jdbc.update("delete from " + table + " where user_id = " + owner, username);
        }
        jdbc.update("delete from users where username = ?", username);
    }

    @Test
    void newUserRegistersVerifiesLogsInAndTracksAnExpense() {
        RestClient http = RestClient.builder().baseUrl("http://localhost:" + port)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> { }).build();

        assertThat(http.post().uri("/auth/register").header("Content-Type", "application/json")
                .body("{\"username\":\"%s\",\"email\":\"%s@example.test\",\"password\":\"secret123\"}"
                        .formatted(username, username))
                .retrieve().toBodilessEntity().getStatusCode().value()).isEqualTo(201);

        assertThat(login(http).getStatusCode().value()).isEqualTo(403);

        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(mail.capture());
        String body = mail.getValue().getText();
        String token = body.substring(body.indexOf("?token=") + 7).trim();
        assertThat(http.post().uri("/auth/verify-email?token={t}", token).retrieve().toBodilessEntity()
                .getStatusCode().value()).isEqualTo(204);

        var loggedIn = login(http);
        assertThat(loggedIn.getStatusCode().value()).isEqualTo(200);
        String access = JsonPath.read(loggedIn.getBody(), "$.accessToken");
        assertThat(loggedIn.getHeaders().getFirst("Set-Cookie")).contains("refresh_token=", "HttpOnly");

        var created = http.post().uri("/api/expenses").header("Authorization", "Bearer " + access)
                .header("Content-Type", "application/json")
                .body("{\"title\":\"Pranzo\",\"amount\":15.00,\"category\":\"other\",\"date\":\"%s\"}"
                        .formatted(LocalDate.now()))
                .retrieve().toEntity(String.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);

        String summary = http.get().uri("/api/dashboard/summary").header("Authorization", "Bearer " + access)
                .retrieve().body(String.class);
        assertThat(JsonPath.<Number>read(summary, "$.totalExpense").doubleValue()).isEqualTo(15.0);
        assertThat(JsonPath.<Number>read(summary, "$.balance").doubleValue()).isEqualTo(-15.0);

        assertThat(http.get().uri("/api/expenses").retrieve().toBodilessEntity().getStatusCode().value())
                .isEqualTo(401);
    }

    private org.springframework.http.ResponseEntity<String> login(RestClient http) {
        return http.post().uri("/auth/login").header("Content-Type", "application/json")
                .body("{\"username\":\"%s\",\"password\":\"secret123\"}".formatted(username))
                .retrieve().toEntity(String.class);
    }
}

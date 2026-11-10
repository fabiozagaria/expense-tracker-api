package org.esercizi.expensetrackerapi;

import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.services.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Non transazionale di proposito: la revoca di tutte le sessioni deve essere COMMITTATA anche
 * quando la richiesta fallisce con 401. In un test transazionale questo non sarebbe verificabile.
 */
@SpringBootTest
class RefreshTokenReuseDetectionTests {
    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    private String username;

    @BeforeEach
    void createUser() {
        username = "reuse" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from refresh where user_id in (select id from users where username = ?)", username);
        jdbc.update("delete from users where username = ?", username);
    }

    @Test
    void reuseRightAfterRotationRejectsOnlyTheLateRequest() throws Exception {
        String first = login();
        String second = authService.refresh(first).refresh();

        // Entro la finestra di tolleranza: è una gara tra due richieste, non un furto.
        assertThatThrownBy(() -> authService.refresh(first)).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(authService.refresh(second).refresh()).as("la sessione valida continua a funzionare").isNotNull();
        assertThat(activeTokens()).isEqualTo(1);
    }

    @Test
    void lateReuseOfARotatedTokenRevokesEveryActiveSession() throws Exception {
        String stolen = login();
        String legitimate = authService.refresh(stolen).refresh();
        String otherDevice = login();
        assertThat(activeTokens()).isEqualTo(2);

        // Il token ruotato risale a oltre la finestra di tolleranza (relativo al valore salvato:
        // niente conversioni di fuso orario).
        jdbc.update("update refresh set revoke_at = revoke_at - interval 1 minute "
                + "where revoke_at is not null and user_id = (select id from users where username = ?)", username);

        assertThatThrownBy(() -> authService.refresh(stolen)).isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(activeTokens()).as("la revoca è stata committata nonostante l'eccezione").isZero();
        assertThatThrownBy(() -> authService.refresh(legitimate)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh(otherDevice)).isInstanceOf(InvalidRefreshTokenException.class);
    }

    private String login() throws Exception {
        return authService.login(new LoginRequest(username, "secret123")).refresh();
    }

    private int activeTokens() {
        Integer count = jdbc.queryForObject(
                "select count(*) from refresh where user_id = (select id from users where username = ?) and revoke_at is null",
                Integer.class, username);
        return count == null ? 0 : count;
    }
}

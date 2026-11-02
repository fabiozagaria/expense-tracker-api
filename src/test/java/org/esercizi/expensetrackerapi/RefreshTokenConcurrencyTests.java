package org.esercizi.expensetrackerapi;

import org.esercizi.expensetrackerapi.dto.login.AccessAndRefresh;
import org.esercizi.expensetrackerapi.dto.login.LoginRequest;
import org.esercizi.expensetrackerapi.exceptions.InvalidRefreshTokenException;
import org.esercizi.expensetrackerapi.model.user.Role;
import org.esercizi.expensetrackerapi.model.user.User;
import org.esercizi.expensetrackerapi.repository.UserRepository;
import org.esercizi.expensetrackerapi.services.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non transazionale di proposito: servono commit reali su thread distinti.
 * Usa un utente con username univoco e lo rimuove a fine test.
 */
@SpringBootTest
class RefreshTokenConcurrencyTests {
    private static final int PARALLEL_REQUESTS = 8;

    @Autowired AuthService authService;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mailSender;

    private String username;

    @AfterEach
    void cleanUp() {
        if (username == null) return;
        jdbc.update("delete from refresh where user_id in (select id from users where username = ?)", username);
        jdbc.update("delete from users where username = ?", username);
    }

    @RepeatedTest(5)
    void sameRefreshTokenCanBeRotatedOnlyOnceUnderConcurrency() throws Exception {
        username = "race" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        users.saveAndFlush(User.builder().username(username).email(username + "@example.test")
                .password(encoder.encode("secret123")).role(Role.USER).emailVerified(true).build());
        String rawRefresh = authService.login(new LoginRequest(username, "secret123")).refresh();

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        CountDownLatch ready = new CountDownLatch(PARALLEL_REQUESTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < PARALLEL_REQUESTS; i++) {
                Callable<Boolean> request = () -> {
                    ready.countDown();
                    go.await();
                    try {
                        AccessAndRefresh rotated = authService.refresh(rawRefresh);
                        return rotated.refresh() != null;
                    } catch (InvalidRefreshTokenException expected) {
                        return false;
                    }
                };
                results.add(pool.submit(request));
            }
            ready.await();
            go.countDown();

            long succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) succeeded++;
            }
            assertThat(succeeded).as("richieste che hanno ruotato lo stesso refresh token").isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        Integer activeTokens = jdbc.queryForObject(
                "select count(*) from refresh where user_id = (select id from users where username = ?) and revoke_at is null",
                Integer.class, username);
        assertThat(activeTokens).as("refresh token ancora attivi dopo la rotazione").isEqualTo(1);
    }
}

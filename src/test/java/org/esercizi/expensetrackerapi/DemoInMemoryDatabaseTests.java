package org.esercizi.expensetrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Il profilo demo non dipende da un database esterno: H2 in memoria, schema creato da Hibernate. */
// REGISTRATION_ENABLED=true (come sul servizio Render attuale) non deve riaprire la registrazione:
// con un database in memoria gli account creati sparirebbero al riavvio.
@SpringBootTest(properties = {"app.demo-password=test-only-demo-password-1234", "REGISTRATION_ENABLED=true"})
@ActiveProfiles("demo")
class DemoInMemoryDatabaseTests {
    @Value("${spring.datasource.url}") String url;
    @Value("${spring.flyway.enabled}") boolean flywayEnabled;
    @Value("${app.registration-enabled}") boolean registrationEnabled;
    @Autowired JdbcTemplate jdbc;

    @Test
    void demoProfileUsesInMemoryDatabaseSeededAtStartup() {
        assertThat(url).startsWith("jdbc:h2:mem:");
        assertThat(flywayEnabled).isFalse();
        assertThat(registrationEnabled).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from users where username = 'portfolio-demo'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from expenses", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from incomes", Integer.class)).isEqualTo(1);
    }
}

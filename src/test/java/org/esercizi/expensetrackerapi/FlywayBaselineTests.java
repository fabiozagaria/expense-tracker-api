package org.esercizi.expensetrackerapi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.mysql.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica il passaggio da Hibernate (ddl-auto=update) a Flyway sui database già esistenti.
 * Usa un container proprio perché ogni test azzera lo schema. La configurazione di Flyway
 * replica quella di application.properties (baseline-on-migrate, baseline-version=1).
 */
class FlywayBaselineTests {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    private Flyway flyway;

    @BeforeAll
    static void startContainer() {
        MYSQL.start();
    }

    @AfterAll
    static void stopContainer() {
        MYSQL.stop();
    }

    @BeforeEach
    void freshFlyway() {
        flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .cleanDisabled(false)
                .load();
        flyway.clean();
    }

    @Test
    void emptyDatabaseRunsEveryMigration() throws SQLException {
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);

        assertThat(tables()).contains("users", "refresh", "verification_tokens",
                "expenses", "incomes", "password_reset_tokens");
        assertThat(history()).containsExactly("1:SQL", "2:SQL");
    }

    @Test
    void existingHibernateDatabaseIsBaselinedAndOnlyNewMigrationsRun() throws SQLException {
        // Stato di produzione prima di Flyway: tabelle create da Hibernate, nessuna password_reset_tokens.
        runScript("db/migration/V1__baseline_schema.sql");
        assertThat(tables()).doesNotContain("password_reset_tokens");

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        assertThat(tables()).contains("password_reset_tokens");
        assertThat(history()).containsExactly("1:BASELINE", "2:SQL");
    }

    @Test
    void developmentDatabaseThatAlreadyHasThePasswordResetTableStillMigrates() throws SQLException {
        // Database di sviluppo su cui ddl-auto=update aveva già creato anche password_reset_tokens.
        runScript("db/migration/V1__baseline_schema.sql");
        runScript("db/migration/V2__password_reset_tokens.sql");

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(history()).containsExactly("1:BASELINE", "2:SQL");
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private void runScript(String classpathLocation) throws SQLException {
        try (Connection c = connection()) {
            ScriptUtils.executeSqlScript(c, new ClassPathResource(classpathLocation));
        }
    }

    private List<String> tables() throws SQLException {
        return query("select table_name from information_schema.tables where table_schema = database()");
    }

    /** "versione:tipo" in ordine di applicazione, es. "1:BASELINE". */
    private List<String> history() throws SQLException {
        return query("select concat(`version`, ':', `type`) from flyway_schema_history "
                + "where success = 1 order by installed_rank");
    }

    private List<String> query(String sql) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (Connection c = connection(); var st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(rs.getString(1));
            }
        }
        return rows;
    }
}

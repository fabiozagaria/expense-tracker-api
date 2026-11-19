package org.esercizi.expensetrackerapi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le migrazioni Flyway su un PostgreSQL vuoto, come il primo avvio su Neon. Container proprio
 * perché ogni test azzera lo schema.
 */
class FlywayMigrationTests {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private Flyway flyway;

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @BeforeEach
    void freshFlyway() {
        flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        flyway.clean();
    }

    @Test
    void emptyDatabaseRunsEveryMigration() throws SQLException {
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        assertThat(query("select table_name from information_schema.tables where table_schema = 'public'"))
                .contains("users", "refresh", "verification_tokens", "password_reset_tokens", "expenses", "incomes");
        assertThat(query("select version || ':' || type from flyway_schema_history where success order by installed_rank"))
                .containsExactly("1:SQL");
    }

    @Test
    void usernameAndEmailAreUniqueIgnoringCase() throws SQLException {
        flyway.migrate();
        execute("insert into users (email, email_verified, password, role, username) "
                + "values ('Mario@Example.test', true, 'x', 'USER', 'MarioRossi')");

        assertThatThrownBy(() -> execute("insert into users (email, email_verified, password, role, username) "
                + "values ('other@example.test', true, 'x', 'USER', 'mariorossi')"))
                .hasMessageContaining("ux_users_username_lower");
        assertThatThrownBy(() -> execute("insert into users (email, email_verified, password, role, username) "
                + "values ('mario@example.TEST', true, 'x', 'USER', 'luigiverdi')"))
                .hasMessageContaining("ux_users_email_lower");
    }

    @Test
    void unknownCategoryIsRejectedByTheDatabase() throws SQLException {
        flyway.migrate();
        execute("insert into users (email, email_verified, password, role, username) "
                + "values ('cat@example.test', true, 'x', 'USER', 'categoryuser')");

        assertThatThrownBy(() -> execute("insert into expenses (amount, category, date, title, user_id) "
                + "select 1.00, 'unknown', current_date, 'Prova', id from users"))
                .hasMessageContaining("expenses_category_check");
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void execute(String sql) throws SQLException {
        try (Connection c = connection(); var st = c.createStatement()) {
            st.execute(sql);
        }
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

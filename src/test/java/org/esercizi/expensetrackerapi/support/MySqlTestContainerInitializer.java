package org.esercizi.expensetrackerapi.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Fornisce a ogni contesto Spring dei test un MySQL usa-e-getta, condiviso da tutti i contesti
 * (si avvia una volta sola per JVM; Ryuk lo rimuove a fine esecuzione).
 *
 * Registrato in META-INF/spring.factories dei test. Le proprietà applicate qui hanno la precedenza
 * su variabili d'ambiente e application.properties: i test non possono più colpire un database
 * "vero" per errore (DB_URL, DB_PASSWORD e SECRET_KEY dell'ambiente vengono ignorati).
 */
public class MySqlTestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    static {
        MYSQL.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                "spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "spring.datasource.username=" + MYSQL.getUsername(),
                "spring.datasource.password=" + MYSQL.getPassword(),
                "jwt.secret=test-only-secret-key-that-is-at-least-32-bytes-long"
        ).applyTo(context);
        // Tutti i test chiamano /auth/login dallo stesso "IP" (127.0.0.1) molte volte al minuto: il limite
        // è spento di default. Chi lo vuole provare lo accende con @SpringBootTest(properties = ...):
        // si guarda solo la sorgente delle proprietà inline, perché application.properties lo definisce già.
        var inline = context.getEnvironment().getPropertySources()
                .get(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
        if (inline == null || !inline.containsProperty("app.rate-limit.enabled")) {
            TestPropertyValues.of("app.rate-limit.enabled=false").applyTo(context);
        }
    }
}

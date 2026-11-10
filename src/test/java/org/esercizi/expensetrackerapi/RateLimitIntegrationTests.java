package org.esercizi.expensetrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Il limite è acceso solo qui (negli altri test è spento). Ogni test usa un proprio IP: il limitatore
 * è condiviso dal contesto Spring e i bucket sono per IP.
 */
@SpringBootTest(properties = "app.rate-limit.enabled=true")
@AutoConfigureMockMvc
class RateLimitIntegrationTests {
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mailSender;

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void badLogin(String ip, int expectedStatus) throws Exception {
        mvc.perform(post("/auth/login").with(from(ip)).contentType("application/json")
                        .content("{\"username\":\"nobodyhere1\",\"password\":\"wrongpass1\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void theEleventhLoginAttemptInAMinuteGets429InTheApiErrorFormat() throws Exception {
        for (int i = 0; i < 10; i++) {
            badLogin("10.1.0.1", 401);
        }

        mvc.perform(post("/auth/login").with(from("10.1.0.1")).contentType("application/json")
                        .content("{\"username\":\"nobodyhere1\",\"password\":\"wrongpass1\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")))
                .andExpect(jsonPath("$.errorName").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.status").value("429 TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void anotherClientIsNotAffected() throws Exception {
        for (int i = 0; i < 10; i++) {
            badLogin("10.2.0.1", 401);
        }
        badLogin("10.2.0.1", 429);

        badLogin("10.2.0.2", 401);
    }

    @Test
    void otherCategoriesAndNonAuthEndpointsAreNotAffectedByAnExhaustedLoginBucket() throws Exception {
        for (int i = 0; i < 10; i++) {
            badLogin("10.3.0.1", 401);
        }
        badLogin("10.3.0.1", 429);

        // Sessione: categoria diversa, nessun cookie => 401 normale, non 429.
        mvc.perform(post("/auth/refresh").with(from("10.3.0.1"))).andExpect(status().isUnauthorized());
        // Solo le POST su /auth sono limitate.
        mvc.perform(get("/health").with(from("10.3.0.1"))).andExpect(status().isOk());
    }

    @Test
    void emailEndpointsShareAFiveAnHourBucket() throws Exception {
        String body = "{\"email\":\"someone@example.test\"}";
        for (int i = 0; i < 5; i++) {
            // Risposta 202 sempre uguale: l'endpoint non rivela se l'email esiste.
            mvc.perform(post("/auth/forgot-password").with(from("10.4.0.1")).contentType("application/json").content(body))
                    .andExpect(status().isAccepted());
        }

        // La sesta, anche su un altro endpoint della stessa categoria, è respinta.
        mvc.perform(post("/auth/resend-verification").with(from("10.4.0.1")).contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorName").value("RATE_LIMITED"));
    }
}

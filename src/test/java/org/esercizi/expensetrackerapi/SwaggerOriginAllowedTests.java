package org.esercizi.expensetrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Profilo demo (controllo d'origine attivo) con Swagger UI accesa e URL pubblico del backend configurato:
 * il "Try it out" della UI parte da quell'origine e deve poter chiamare /auth/demo.
 */
@SpringBootTest(properties = {
        "app.demo-password=test-only-demo-password-1234",
        "spring.datasource.hikari.data-source-properties.sslMode=DISABLED",
        "app.api-public-url=https://api.example.test/",
        "springdoc.swagger-ui.enabled=true"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Transactional
class SwaggerOriginAllowedTests {
    @Autowired MockMvc mvc;

    @Test
    void theBackendOwnOriginCanCallAuthEndpointsFromSwaggerUi() throws Exception {
        // L'URL è configurato con lo slash finale: si confronta senza.
        mvc.perform(post("/auth/demo").header("Origin", "https://api.example.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void theFrontendOriginStillWorks() throws Exception {
        mvc.perform(post("/auth/demo").header("Origin", "http://localhost:4200")).andExpect(status().isOk());
    }

    @Test
    void anyOtherOriginOrNoOriginIsRejected() throws Exception {
        mvc.perform(post("/auth/demo").header("Origin", "https://evil.example")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/demo").header("Origin", "https://api.example.test.evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/demo")).andExpect(status().isForbidden());
    }
}

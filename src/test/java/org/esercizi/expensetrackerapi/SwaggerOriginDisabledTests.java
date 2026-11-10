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

/** Con la Swagger UI spenta l'origine del backend non è più ammessa: resta solo il frontend. */
@SpringBootTest(properties = {
        "app.demo-password=test-only-demo-password-1234",
        "spring.datasource.hikari.data-source-properties.sslMode=DISABLED",
        "app.api-public-url=https://api.example.test",
        "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Transactional
class SwaggerOriginDisabledTests {
    @Autowired MockMvc mvc;

    @Test
    void theBackendOriginIsRejectedWhenSwaggerUiIsOff() throws Exception {
        mvc.perform(post("/auth/demo").header("Origin", "https://api.example.test")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/demo").header("Origin", "http://localhost:4200")).andExpect(status().isOk());
    }
}

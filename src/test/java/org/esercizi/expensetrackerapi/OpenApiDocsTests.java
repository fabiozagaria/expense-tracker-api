package org.esercizi.expensetrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La documentazione OpenAPI è pubblica (senza token) e descrive correttamente la sicurezza. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTests {
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void apiDocsAreServedWithoutAuthenticationAndDescribeTheContract() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Gestionale Spese API"))
                // La versione arriva dal pom (filtro Maven): non deve restare il segnaposto.
                .andExpect(jsonPath("$.info.version").value(not(containsString("@"))))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/expenses']").exists())
                .andExpect(jsonPath("$.paths['/api/incomes']").exists())
                .andExpect(jsonPath("$.paths['/auth/reset-password']").exists());
    }

    @Test
    void protectedEndpointsRequireTheBearerTokenAndPublicOnesDoNot() throws Exception {
        // Requisito globale: bearerAuth; gli endpoint pubblici lo azzerano con @SecurityRequirements.
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/expenses'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/health'].get.security").value(empty()));
    }

    @Test
    void listEndpointsDocumentTheTruncationHeader() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/expenses'].get.description").value(containsString("X-Result-Truncated")))
                .andExpect(jsonPath("$.paths['/api/incomes'].get.description").value(containsString("X-Result-Truncated")));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }
}

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
    void errorResponsesAreDeclaredWhereTheyCanHappen() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                // Lo schema degli errori è registrato e le risposte lo referenziano.
                .andExpect(jsonPath("$.components.schemas.APIError.properties.errorName").exists())
                .andExpect(jsonPath("$.paths['/api/expenses'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/APIError"))
                // Endpoint protetto: 401 senza corpo; con parametri: 400.
                .andExpect(jsonPath("$.paths['/api/expenses'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/expenses/search'].get.responses['400']").exists())
                // Login: errori specifici dalle annotazioni, più 400 e 429 dal customizer.
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['401'].description").value(containsString("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['403'].description").value(containsString("EMAIL_NOT_VERIFIED")))
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['429'].headers['Retry-After']").exists())
                .andExpect(jsonPath("$.paths['/auth/forgot-password'].post.responses['429']").exists())
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.responses['401'].description").value(containsString("INVALID_REFRESH_TOKEN")))
                .andExpect(jsonPath("$.paths['/auth/refresh'].post.responses['429']").exists());
    }

    @Test
    void errorsAreNotDeclaredWhereTheyCannotHappen() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                // Pubblico e senza input: nessun 400, 401 o 429. Le GET non sono limitate dal rate limiter.
                .andExpect(jsonPath("$.paths['/health'].get.responses['400']").doesNotExist())
                .andExpect(jsonPath("$.paths['/health'].get.responses['401']").doesNotExist())
                .andExpect(jsonPath("$.paths['/health'].get.responses['429']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/expenses'].get.responses['429']").doesNotExist())
                // Il login è pubblico: il suo 401 è quello delle credenziali, non quello del token.
                .andExpect(jsonPath("$.paths['/auth/login'].post.responses['401'].description").value(not(containsString("Token di accesso"))));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }
}

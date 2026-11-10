package org.esercizi.expensetrackerapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI expenseTrackerOpenApi(@Value("${app.version:dev}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestionale Spese API")
                        .version(version)
                        .description("""
                                Backend REST del Gestionale Spese: registrazione con verifica email, login JWT \
                                con refresh token in cookie, spese, entrate e dashboard.

                                Per provare gli endpoint protetti: `POST /auth/login`, copia l'`accessToken` \
                                e inseriscilo con il pulsante **Authorize**.""")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                // Di default ogni endpoint richiede il token; quelli pubblici lo escludono con @SecurityRequirements.
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}

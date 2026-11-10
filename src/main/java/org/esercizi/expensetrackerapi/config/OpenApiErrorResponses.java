package org.esercizi.expensetrackerapi.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.esercizi.expensetrackerapi.exceptions.errors.APIError;
import org.esercizi.expensetrackerapi.security.ratelimit.RateLimitPolicy;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Dichiara nella specifica le risposte d'errore comuni, senza annotare ogni endpoint:
 * <ul>
 *   <li><b>400</b> dove c'è un corpo o un parametro da validare (formato {@code APIError});</li>
 *   <li><b>401</b> sugli endpoint protetti da bearer (corpo vuoto: lo produce il resource server);</li>
 *   <li><b>429</b> sulle POST limitate da {@link RateLimitPolicy}, con l'header {@code Retry-After}.</li>
 * </ul>
 * Una risposta già dichiarata con le annotazioni (es. il 401 di login) non viene sovrascritta.
 */
@Component
public class OpenApiErrorResponses implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        ModelConverters.getInstance().readAll(APIError.class).forEach(openApi.getComponents()::addSchemas);
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, operation) -> addErrors(path, method, operation)));
    }

    private void addErrors(String path, PathItem.HttpMethod method, Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        ApiResponses responses = operation.getResponses();

        boolean hasInput = operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
        if (hasInput) {
            responses.addApiResponse("400", responses.getOrDefault("400", apiError(
                    "Richiesta non valida: VALIDATION_ERROR (campi sbagliati), MALFORMED_REQUEST (JSON illeggibile) "
                            + "o INVALID_PARAMETER (tipo errato)")));
        }

        // Il requisito globale lascia security a null; gli endpoint pubblici lo azzerano con una lista vuota.
        boolean protectedByBearer = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
        if (protectedByBearer && !responses.containsKey("401")) {
            responses.addApiResponse("401", new ApiResponse()
                    .description("Token di accesso mancante, non valido o scaduto (corpo vuoto)"));
        }

        if (method == PathItem.HttpMethod.POST && RateLimitPolicy.forPath(path).isPresent()
                && !responses.containsKey("429")) {
            responses.addApiResponse("429", apiError("Troppe richieste (RATE_LIMITED): riprovare dopo i secondi indicati")
                    .addHeaderObject("Retry-After", new Header()
                            .description("Secondi da attendere prima di riprovare")
                            .schema(new IntegerSchema())));
        }
    }

    private static ApiResponse apiError(String description) {
        Schema<?> reference = new Schema<>().$ref("#/components/schemas/APIError");
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(reference)));
    }
}

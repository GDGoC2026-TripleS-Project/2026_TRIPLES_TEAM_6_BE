package com.lastcup.api.global.config;

import com.lastcup.api.global.response.ApiResponseError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openApi() {
        Components components = new Components()
                .addSecuritySchemes("BearerAuth", bearerAuthScheme());
        return new OpenAPI()
                .components(components);
    }

    @Bean
    public OpenApiCustomizer apiResponseCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            Map<String, Schema> schemas = ModelConverters.getInstance().read(ApiResponseError.class);
            schemas.forEach(components::addSchemas);

            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        addErrorResponse(operation, "400", "Bad Request");
                        addErrorResponse(operation, "401", "Unauthorized");
                        addErrorResponse(operation, "403", "Forbidden");
                        addErrorResponse(operation, "409", "Conflict");
                        addErrorResponse(operation, "500", "Internal Server Error");
                    })
            );
        };
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name("BearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    private void addErrorResponse(io.swagger.v3.oas.models.Operation operation, String status, String description) {
        if (operation.getResponses() == null) {
            operation.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
        }
        if (operation.getResponses().containsKey(status)) {
            return;
        }
        ApiResponse apiResponse = new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiResponseError"))
                ));
        operation.getResponses().addApiResponse(status, apiResponse);
    }
}

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
import io.swagger.v3.oas.models.servers.Server;
import java.util.Map;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class SwaggerConfig {

    private final String swaggerServerUrl;
    private final String swaggerServerDescription;
    private final String swaggerLocalServerUrl;
    private final String swaggerLocalServerDescription;

    public SwaggerConfig(
            @Value("${app.swagger.server-url:https://api.lastcup.site}") String swaggerServerUrl,
            @Value("${app.swagger.server-description:API Server}") String swaggerServerDescription,
            @Value("${app.swagger.local-server-url:http://localhost:8080}") String swaggerLocalServerUrl,
            @Value("${app.swagger.local-server-description:Local Server}") String swaggerLocalServerDescription
    ) {
        this.swaggerServerUrl = swaggerServerUrl;
        this.swaggerServerDescription = swaggerServerDescription;
        this.swaggerLocalServerUrl = swaggerLocalServerUrl;
        this.swaggerLocalServerDescription = swaggerLocalServerDescription;
    }

    @Bean
    public OpenAPI openApi() {
        Components components = new Components()
                .addSecuritySchemes("BearerAuth", bearerAuthScheme());
        return new OpenAPI()
                .components(components)
                .servers(List.of(
                        new Server()
                                .url(swaggerServerUrl)
                                .description(swaggerServerDescription),
                        new Server()
                                .url(swaggerLocalServerUrl)
                                .description(swaggerLocalServerDescription)
                ));
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

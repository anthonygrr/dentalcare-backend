package com.dentalcare.tony.api.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata served at {@code /v3/api-docs} and rendered by
 * Swagger UI at {@code /swagger-ui.html} (both paths are permitted
 * in {@link SecurityConfig}).
 *
 * <p>The global security requirement makes Swagger UI's "Authorize"
 * button issue the JWT as a {@code Authorization: Bearer} header on
 * every operation; log in through {@code POST /api/auth/login} first.</p>
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI dentalCareOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DentalCare API")
                        .description("""
                                REST API for a dental clinic: patients, doctors,
                                appointments and medical records.

                                Authenticate via `POST /api/auth/login` and use the
                                "Authorize" button to send the JWT on protected
                                endpoints. Rate limits apply (5/min on login,
                                60/min per client on the rest).""")
                        .version("0.0.1-SNAPSHOT"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

package com.tdetroy.valuacion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación interactiva de la API (tasks.md T0.4): metadatos expuestos en
 * {@code /v3/api-docs} y la UI en {@code /swagger-ui.html} (springdoc-openapi, generado a
 * partir de las anotaciones de {@code controllers/}/{@code dto/} de cada fase, sin
 * mantenimiento manual de un contrato aparte).
 *
 * <p>Declara ya el esquema de seguridad Bearer/JWT como metadato documental — no habilita
 * ninguna validación real todavía; el filtro que efectivamente valida el token es de T1.4.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    OpenAPI valuacionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Valuación de Jugadores de Fútbol")
                        .description("Tokenización interna, cotización algorítmica basada en "
                                + "rendimiento y portfolio de jugadores de fútbol profesionales.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}

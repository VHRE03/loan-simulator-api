package com.vhre.loansimulator.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Global OpenAPI (Swagger) documentation configuration.
 *
 * <p>The server URL used by the Swagger UI "Try it out" feature is
 * configurable per environment through the properties:</p>
 * <ul>
 *   <li>{@code openapi.server.url}</li>
 *   <li>{@code openapi.server.description}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Value("${openapi.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${openapi.server.description:Servidor Local}")
    private String serverDescription;

    @Bean
    public OpenAPI loanSimulatorOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Loan Simulator API")
                        .description("API REST para la simulacion de prestamos: cuotas, intereses y plan de amortizacion.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Equipo de Arquitectura").email("arch@vhre.com"))
                        .license(new License().name("Propiedad de VHRE").url("https://vhre.com")))
                .servers(List.of(new Server().url(serverUrl).description(serverDescription)))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

package com.vhre.loansimulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link OpenApiConfig} using plain JUnit 5 (no Spring context).
 *
 * <p>The {@code @Value} fields are injected by Spring at runtime; in this
 * isolated unit test we set them directly with {@link ReflectionTestUtils}.</p>
 */
class OpenApiConfigTest {

    private OpenApiConfig openApiConfig;

    @BeforeEach
    void setUp() {
        openApiConfig = new OpenApiConfig();
        ReflectionTestUtils.setField(openApiConfig, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(openApiConfig, "serverDescription", "Local Server");
    }

    @Test
    @DisplayName("The OpenAPI bean exposes the project metadata")
    void openApiBeanExposesProjectMetadata() {
        OpenAPI openAPI = openApiConfig.loanSimulatorOpenAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Loan Simulator API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
    }

    @Test
    @DisplayName("The OpenAPI bean exposes the configured server for Swagger UI Try it out")
    void openApiBeanExposesConfiguredServer() {
        OpenAPI openAPI = openApiConfig.loanSimulatorOpenAPI();

        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8080");
        assertThat(openAPI.getServers().get(0).getDescription()).isEqualTo("Local Server");
    }

    @Test
    @DisplayName("The OpenAPI bean registers the bearer JWT security scheme")
    void openApiBeanRegistersSecurityScheme() {
        OpenAPI openAPI = openApiConfig.loanSimulatorOpenAPI();

        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getSecurity()).isNotEmpty();
    }
}

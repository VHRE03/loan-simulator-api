package com.vhre.loansimulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test: verifies that the Spring application context loads.
 *
 * <p>This test starts the full application, so it requires a reachable
 * PostgreSQL instance as configured in {@code application-local.properties}
 * (Flyway will apply any pending migration during startup). Unit tests that do
 * not require a database live next to this class (see
 * {@code config.OpenApiConfigTest} and {@code pattern.ServiceTestPatternTest}).</p>
 */
@SpringBootTest
class LoanSimulatorApiApplicationTests {

    @Test
    void contextLoads() {
    }

}

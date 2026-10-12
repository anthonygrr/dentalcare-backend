package com.dentalcare.tony.api.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Boots the full application against a throwaway PostgreSQL container
 * (same image as ../docker-compose.yml). No local database or env vars
 * are needed: {@code @ServiceConnection} injects the datasource, and
 * {@code jwt.secret} is overridden per test class because
 * {@code application.properties} expects {@code ${JWT_SECRET}}.
 * Rate limiting is disabled in tests: it has its own unit tests, and
 * the 5/min login bucket would make suites flaky.
 *
 * <p>The container is a JVM-wide singleton (NOT {@code @Container}):
 * the three integration test classes share one Spring context cache,
 * and per-class container stop/start would restart Postgres on a new
 * port while the cached context still points at the dead one.
 * Ryuk reaps the container at JVM exit.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secret=integration-test-secret-that-is-long-enough-for-hs256!!",
                "rate-limit.enabled=false"
        })
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:14-alpine");

    @BeforeAll
    static void startSharedContainer() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }
}

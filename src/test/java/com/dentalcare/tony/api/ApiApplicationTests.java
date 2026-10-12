package com.dentalcare.tony.api;

import com.dentalcare.tony.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Boots the whole application (beans, JPA, security, seed) against
 * Testcontainers PostgreSQL — no local database needed anymore.
 */
class ApiApplicationTests extends PostgresIntegrationTest {

	@Test
	void contextLoads() {
	}

}

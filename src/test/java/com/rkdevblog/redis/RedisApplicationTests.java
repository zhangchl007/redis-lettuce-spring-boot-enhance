package com.rkdevblog.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test to verify that the Spring Boot application context loads successfully.
 *
 * - This test will fail if any bean cannot be created, configuration is invalid, or required properties are missing.
 * - It is a best practice to keep this test empty: any exception during context startup will fail the test.
 * - If you want to verify specific beans or profiles, add assertions or use @ActiveProfiles.
 * - If you want to test that the application context loads with a specific profile, uncomment @ActiveProfiles below.
 */
// @ActiveProfiles("test") // Uncomment if you want to load a specific profile for context loading

@SpringBootTest
class RedisApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty.
        // This test passes if the Spring application context loads without exceptions.
        // If the context fails to load, this test will fail.
        assertTrue(true, "Spring context loaded successfully.");
    }
}

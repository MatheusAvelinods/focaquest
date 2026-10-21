package com.focaquest.support;

import java.time.Instant;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

/**
 * Base dos testes de integração. Usa um PostgreSQL efêmero via Testcontainers; se a variável
 * FOCAQUEST_TEST_DB_URL estiver definida (ex.: CI sem Docker), usa esse banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTest.TestClockConfig.class)
public abstract class IntegrationTest {

    private static final String EXTERNAL_DB = System.getenv("FOCAQUEST_TEST_DB_URL");
    private static PostgreSQLContainer postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (EXTERNAL_DB != null && !EXTERNAL_DB.isBlank()) {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB);
            registry.add("spring.datasource.username", () -> env("FOCAQUEST_TEST_DB_USER", "focaquest"));
            registry.add("spring.datasource.password", () -> env("FOCAQUEST_TEST_DB_PASSWORD", "focaquest"));
            return;
        }
        synchronized (IntegrationTest.class) {
            if (postgres == null) {
                postgres = new PostgreSQLContainer("postgres:16-alpine");
                postgres.start();
            }
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    @TestConfiguration
    public static class TestClockConfig {
        @Bean
        @Primary
        public MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-21T12:00:00Z"));
        }
    }
}

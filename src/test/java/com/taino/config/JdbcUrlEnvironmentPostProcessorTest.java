package com.taino.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JdbcUrlEnvironmentPostProcessorTest {

    @Test
    void prependsJdbcToPostgresqlUrl() {
        assertEquals(
                "jdbc:postgresql://host/db?sslmode=require",
                JdbcUrlEnvironmentPostProcessor.normalizeJdbcUrl("postgresql://host/db?sslmode=require")
        );
    }

    @Test
    void prependsJdbcToPostgresUrl() {
        assertEquals(
                "jdbc:postgresql://host/db",
                JdbcUrlEnvironmentPostProcessor.normalizeJdbcUrl("postgres://host/db")
        );
    }

    @Test
    void keepsExistingJdbcUrl() {
        assertEquals(
                "jdbc:postgresql://host/db",
                JdbcUrlEnvironmentPostProcessor.normalizeJdbcUrl("jdbc:postgresql://host/db")
        );
    }

    @Test
    void rewritesNeonChannelBindingParam() {
        assertEquals(
                "jdbc:postgresql://host/db?sslmode=require&channelBinding=require",
                JdbcUrlEnvironmentPostProcessor.normalizeJdbcUrl(
                        "postgresql://host/db?sslmode=require&channel_binding=require"
                )
        );
    }
}

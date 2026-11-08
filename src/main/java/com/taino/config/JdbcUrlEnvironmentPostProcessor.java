package com.taino.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Accepts Neon/libpq URLs ({@code postgresql://...} / {@code postgres://...})
 * and rewrites them to a JDBC URL Spring Boot expects.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JdbcUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String SOURCE = "tainoJdbcUrlNormalization";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("DATABASE_URL")
        );
        if (raw == null) {
            return;
        }

        String normalized = normalizeJdbcUrl(raw);
        if (normalized.equals(raw)
                && environment.getProperty("SPRING_DATASOURCE_URL") != null
                && environment.getProperty("DATABASE_URL") == null) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", normalized);
        props.put("SPRING_DATASOURCE_URL", normalized);
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
    }

    static String normalizeJdbcUrl(String url) {
        String value = url.trim();
        if (value.startsWith("postgres://")) {
            value = "jdbc:postgresql://" + value.substring("postgres://".length());
        } else if (value.startsWith("postgresql://")) {
            value = "jdbc:postgresql://" + value.substring("postgresql://".length());
        } else if (!value.startsWith("jdbc:") && value.contains("://")) {
            // unknown scheme with host — leave as-is; Spring will fail loudly
            return value;
        }

        // libpq param from Neon connection strings; pgJDBC uses channelBinding instead
        value = value.replace("channel_binding=require", "channelBinding=require");
        value = value.replace("channel_binding=disable", "channelBinding=disable");
        value = value.replace("channel_binding=prefer", "channelBinding=prefer");
        return value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

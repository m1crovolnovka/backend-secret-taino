package com.taino.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "taino")
public class TainoProperties {

    private final Telegram telegram = new Telegram();
    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Imgbb imgbb = new Imgbb();
    private String miniAppUrl = "http://localhost:5173";

    public Telegram getTelegram() {
        return telegram;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Imgbb getImgbb() {
        return imgbb;
    }

    public String getMiniAppUrl() {
        return miniAppUrl;
    }

    public void setMiniAppUrl(String miniAppUrl) {
        this.miniAppUrl = miniAppUrl;
    }

    public static class Telegram {
        private String botToken = "";
        private String botUsername = "";
        private long initDataMaxAgeSeconds = 3600;
        private String webhookSecret = "";

        public String getBotToken() {
            return botToken;
        }

        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }

        public String getBotUsername() {
            return botUsername;
        }

        public void setBotUsername(String botUsername) {
            this.botUsername = botUsername;
        }

        public long getInitDataMaxAgeSeconds() {
            return initDataMaxAgeSeconds;
        }

        public void setInitDataMaxAgeSeconds(long initDataMaxAgeSeconds) {
            this.initDataMaxAgeSeconds = initDataMaxAgeSeconds;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Jwt {
        private String secret = "";
        private long ttlSeconds = 3600;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";

        public List<String> getAllowedOrigins() {
            return java.util.Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Imgbb {
        private String apiKey = "";
        private String baseUrl = "https://api.imgbb.com";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}

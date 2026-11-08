package com.taino.notification;

import com.taino.config.TainoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final RestClient restClient;
    private final TainoProperties properties;

    public TelegramBotClient(RestClient.Builder builder, TainoProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://api.telegram.org").build();
    }

    public boolean sendMessage(long chatId, String text) {
        String token = properties.getTelegram().getBotToken();
        if (token == null || token.isBlank() || token.contains("ABC-DEF")) {
            log.debug("Telegram send skipped: bot token not configured");
            return false;
        }
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("Telegram sendMessage failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}

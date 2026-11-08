package com.taino.notification;

import com.taino.config.TainoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
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
        return send(chatId, text, null);
    }

    public boolean sendWelcome(long chatId, String text, String webAppUrl, String buttonText) {
        Map<String, Object> markup = null;
        if (StringUtils.hasText(webAppUrl)) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("text", buttonText != null ? buttonText : "Открыть Тайно");
            button.put("web_app", Map.of("url", webAppUrl));
            markup = Map.of(
                    "inline_keyboard", List.of(List.of(button))
            );
        }
        return send(chatId, text, markup);
    }

    private boolean send(long chatId, String text, Map<String, Object> replyMarkup) {
        String token = properties.getTelegram().getBotToken();
        if (token == null || token.isBlank() || token.contains("ABC-DEF")) {
            log.debug("Telegram send skipped: bot token not configured");
            return false;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup);
            }
            restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("Telegram sendMessage failed: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}

package com.taino.bot;

import com.taino.config.TainoProperties;
import com.taino.notification.TelegramBotClient;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bot")
@Hidden
public class BotWebhookController {

    private final TainoProperties properties;
    private final TelegramBotClient botClient;

    public BotWebhookController(TainoProperties properties, TelegramBotClient botClient) {
        this.properties = properties;
        this.botClient = botClient;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secret,
            @RequestBody Map<String, Object> update
    ) {
        String expected = properties.getTelegram().getWebhookSecret();
        if (StringUtils.hasText(expected) && !expected.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Object messageObj = update.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return ResponseEntity.ok().build();
        }
        Object textObj = message.get("text");
        Object chatObj = message.get("chat");
        if (!(textObj instanceof String text) || !(chatObj instanceof Map<?, ?> chat)) {
            return ResponseEntity.ok().build();
        }
        Object chatIdObj = chat.get("id");
        if (!(chatIdObj instanceof Number chatIdNum)) {
            return ResponseEntity.ok().build();
        }
        long chatId = chatIdNum.longValue();

        if (text.startsWith("/start")) {
            String payload = text.length() > 6 ? text.substring(6).trim() : "";
            String roomHint = "";
            if (payload.startsWith("room_")) {
                roomHint = payload.substring("room_".length()).trim();
            }
            String miniApp = properties.getMiniAppUrl();
            String body = "Привет! Это Тайно — анонимный Тайный Санта.\n"
                    + "Открой Mini App кнопкой ниже.";
            if (StringUtils.hasText(roomHint)) {
                body += "\nКод комнаты: <b>" + roomHint + "</b>";
            }
            // WebApp keyboard via sendMessage reply_markup is richer; text + URL is enough for MVP.
            botClient.sendMessage(chatId, body + "\n\nMini App: " + miniApp
                    + (StringUtils.hasText(roomHint) ? ("?startapp=room_" + roomHint) : ""));
        }
        return ResponseEntity.ok().build();
    }
}

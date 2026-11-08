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
            String roomCode = "";
            if (payload.startsWith("room_")) {
                roomCode = payload.substring("room_".length()).trim();
            }

            String miniApp = properties.getMiniAppUrl();
            String webAppUrl = miniApp;
            if (StringUtils.hasText(roomCode)) {
                String sep = miniApp.contains("?") ? "&" : "?";
                webAppUrl = miniApp + sep + "startapp=room_" + roomCode;
            }

            String body = """
                    <b>Тайно</b> — анонимный Тайный Санта в Telegram.

                    Как это работает:
                    1) Заходишь в комнату по ссылке или коду
                    2) Составляешь список желаний (ссылки и фото)
                    3) В срок раздачи тебе открывается <i>чужой</i> список — без имени
                    4) Покупаешь подарок тихо и даришь до даты вручения

                    Админ комнаты приглашает друзей, следит за сроками и запускает раздачу.
                    """;
            if (StringUtils.hasText(roomCode)) {
                body += "\nТебя зовут в комнату с кодом <b>" + roomCode + "</b>. Нажми кнопку ниже — откроется Mini App сразу в этой комнате.";
            } else {
                body += "\nНажми кнопку ниже, чтобы открыть приложение.";
            }

            botClient.sendWelcome(chatId, body.trim(), webAppUrl, StringUtils.hasText(roomCode) ? "Войти в комнату" : "Открыть Тайно");
        }
        return ResponseEntity.ok().build();
    }
}

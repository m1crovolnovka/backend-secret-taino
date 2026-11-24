package com.taino.bot;

import com.taino.config.TainoProperties;
import com.taino.notification.TelegramBotClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * After a user opens the Mini App via startapp (no /start in chat yet),
 * Telegram still allows the bot to message them — we write a welcome so
 * the chat history exists and they can reopen the app from the bot.
 */
@Service
public class BotGreetingService {

    private final TelegramBotClient botClient;
    private final TainoProperties properties;

    public BotGreetingService(TelegramBotClient botClient, TainoProperties properties) {
        this.botClient = botClient;
        this.properties = properties;
    }

    public boolean greet(long telegramUserId, String startParam) {
        String roomCode = extractRoomCode(startParam);
        String trackPayload = extractTrackingStartParam(startParam);
        String miniApp = properties.getMiniAppUrl();
        String webAppUrl = miniApp;
        if (StringUtils.hasText(miniApp)) {
            String sep = miniApp.contains("?") ? "&" : "?";
            if (StringUtils.hasText(roomCode)) {
                webAppUrl = miniApp + sep + "startapp=room_" + roomCode;
            } else if (StringUtils.hasText(trackPayload)) {
                // Keep attribution payload; do NOT treat trk_* as a room invite.
                webAppUrl = miniApp + sep + "startapp=" + trackPayload;
            }
        }

        String body;
        String button;
        if (StringUtils.hasText(roomCode)) {
            body = """
                    Привет! На связи <b>Тайно</b> 🎁✨

                    Тебя зовут в комнату <b>%s</b>.
                    Анонимный обмен подарками: составляешь вишлист, а в раздачу открывается чужой список — без имени, только цвет.

                    Здесь в чате всегда можно снова открыть приложение кнопкой ниже 👇
                    """.formatted(roomCode);
            button = "Открыть комнату 🎁";
        } else {
            body = """
                    Привет! На связи <b>Тайно</b> 🎁✨

                    Анонимный обмен подарками в Telegram.
                    Составь вишлист, дождись раздачи — откроется чужой список без имени.

                    Открывая приложение, ты принимаешь политику конфиденциальности и условия использования
                    (раздел «О сервисе» в меню Mini App).

                    Сохрани этот чат: отсюда всегда можно снова открыть приложение 👇
                    """;
            button = "Открыть Тайно 🎁";
        }

        boolean ok = botClient.sendWelcome(telegramUserId, body.trim(), webAppUrl, button);
        if (ok) {
            botClient.ensureMenuButton();
        }
        return ok;
    }

    /**
     * Only {@code room_CODE} invites into a room. Tracking payloads like {@code trk_…}
     * must never be treated as room codes.
     */
    static String extractRoomCode(String startParam) {
        if (!StringUtils.hasText(startParam)) {
            return "";
        }
        String p = startParam.trim();
        String lower = p.toLowerCase(Locale.ROOT);
        if (lower.startsWith("trk_") || lower.startsWith("t_")) {
            return "";
        }
        if (!lower.startsWith("room_")) {
            return "";
        }
        p = p.substring(5);
        if (p.matches("[A-Za-z0-9_-]{4,32}")) {
            return p.toUpperCase(Locale.ROOT);
        }
        return "";
    }

    static String extractTrackingStartParam(String startParam) {
        if (!StringUtils.hasText(startParam)) {
            return "";
        }
        String p = startParam.trim();
        String lower = p.toLowerCase(Locale.ROOT);
        if (lower.startsWith("trk_") || lower.startsWith("t_")) {
            return p;
        }
        return "";
    }
}

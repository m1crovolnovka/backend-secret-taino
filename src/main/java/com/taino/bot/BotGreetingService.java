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
        String miniApp = properties.getMiniAppUrl();
        String webAppUrl = miniApp;
        if (StringUtils.hasText(roomCode) && StringUtils.hasText(miniApp)) {
            String sep = miniApp.contains("?") ? "&" : "?";
            webAppUrl = miniApp + sep + "startapp=room_" + roomCode;
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

    static String extractRoomCode(String startParam) {
        if (!StringUtils.hasText(startParam)) {
            return "";
        }
        String p = startParam.trim();
        if (p.toLowerCase(Locale.ROOT).startsWith("room_")) {
            p = p.substring(5);
        }
        if (p.matches("[A-Za-z0-9_-]{4,32}")) {
            return p.toUpperCase(Locale.ROOT);
        }
        return "";
    }
}

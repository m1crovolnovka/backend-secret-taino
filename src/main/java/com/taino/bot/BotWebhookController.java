package com.taino.bot;

import com.taino.config.TainoProperties;
import com.taino.notification.TelegramBotClient;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/bot")
@Hidden
public class BotWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BotWebhookController.class);
    private static final Pattern START_CMD = Pattern.compile(
            "^/start(?:@\\w+)?(?:\\s+(.+))?$",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

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
            log.warn("Webhook rejected: secret token mismatch");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Object messageObj = update.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return ResponseEntity.ok().build();
        }
        Object textObj = message.get("text");
        Object chatObj = message.get("chat");
        if (!(textObj instanceof String rawText) || !(chatObj instanceof Map<?, ?> chat)) {
            return ResponseEntity.ok().build();
        }
        // Strip BOM / zero-width chars Telegram sometimes inserts
        String text = rawText.replace("\uFEFF", "").replace("\u200B", "").trim();
        Object chatIdObj = chat.get("id");
        Long chatId = toLong(chatIdObj);
        if (chatId == null) {
            return ResponseEntity.ok().build();
        }

        Matcher start = START_CMD.matcher(text);
        if (!start.matches()) {
            return ResponseEntity.ok().build();
        }

        String payload = start.group(1) != null ? start.group(1).trim() : "";
        String roomCode = extractRoomCode(payload);
        log.info("Handling /start chatId={} room={}", chatId, roomCode.isEmpty() ? "-" : roomCode);

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

                    Тебя зовут в комнату с кодом <b>%s</b>.
                    Анонимный обмен подарками: составляешь вишлист, а в раздачу открывается чужой список — без имени, только цвет.

                    Нажимай кнопку — Mini App откроется сразу в этой комнате 👇
                    """.formatted(roomCode);
            button = "Войти в комнату 🎁";
        } else {
            body = """
                    Привет! На связи <b>Тайно</b> 🎁✨

                    Анонимный обмен подарками в Telegram:
                    1) Заходишь в комнату по ссылке или коду
                    2) Составляешь список желаний
                    3) В срок открывается чужой список — без имени
                    4) Покупаешь подарок тихо и даришь до даты

                    Нажимай кнопку и погнали 👇
                    """;
            button = "Открыть Тайно 🎁";
        }

        boolean sent = botClient.sendWelcome(chatId, body.trim(), webAppUrl, button);
        if (!sent) {
            log.warn("Failed to reply to /start for chatId={}", chatId);
        }
        botClient.ensureMenuButton();
        return ResponseEntity.ok().build();
    }

    static String extractRoomCode(String payload) {
        if (!StringUtils.hasText(payload)) {
            return "";
        }
        String p = payload.trim();
        if (p.toLowerCase(Locale.ROOT).startsWith("room_")) {
            p = p.substring(5);
        }
        if (p.matches("[A-Za-z0-9_-]{4,32}")) {
            return p.toUpperCase(Locale.ROOT);
        }
        return "";
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

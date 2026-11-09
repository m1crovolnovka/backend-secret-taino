package com.taino.notification;

import com.taino.config.TainoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TelegramBotClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final RestClient restClient;
    private final TainoProperties properties;
    private final AtomicBoolean menuReady = new AtomicBoolean(false);

    public TelegramBotClient(RestClient.Builder builder, TainoProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://api.telegram.org").build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        ensureBotProfile();
        ensureBotCommands();
        ensureMenuButton();
        ensureWebhook();
    }

    public boolean sendMessage(long chatId, String text) {
        return send(chatId, text, null);
    }

    public boolean sendWelcome(long chatId, String text, String webAppUrl, String buttonText) {
        Map<String, Object> markup = null;
        // Telegram rejects web_app buttons with non-HTTPS URLs — whole sendMessage fails.
        if (StringUtils.hasText(webAppUrl) && webAppUrl.toLowerCase().startsWith("https://")) {
            Map<String, Object> button = new LinkedHashMap<>();
            button.put("text", buttonText != null ? buttonText : "Открыть Тайно 🎁");
            button.put("web_app", Map.of("url", webAppUrl));
            markup = Map.of(
                    "inline_keyboard", List.of(List.of(button))
            );
        }
        boolean ok = send(chatId, text, markup);
        if (!ok && markup != null) {
            // Retry without keyboard if web_app button was rejected
            log.warn("Welcome with web_app button failed — retrying as plain text");
            return send(chatId, text, null);
        }
        return ok;
    }

    public void ensureWebhook() {
        String token = tokenOrNull();
        if (token == null) return;

        String base = properties.getPublicApiUrl();
        if (!StringUtils.hasText(base)) {
            log.warn("PUBLIC_API_URL is empty — Telegram webhook is NOT registered; /start will do nothing");
            return;
        }
        if (!base.toLowerCase().startsWith("https://")) {
            log.warn("PUBLIC_API_URL must be HTTPS for Telegram webhook, got: {}", base);
            return;
        }
        String webhookUrl = base.replaceAll("/$", "") + "/api/bot/webhook";
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("url", webhookUrl);
            body.put("allowed_updates", List.of("message"));
            body.put("drop_pending_updates", false);
            String secret = properties.getTelegram().getWebhookSecret();
            if (StringUtils.hasText(secret)) {
                body.put("secret_token", secret);
            }
            Map<?, ?> resp = restClient.post()
                    .uri("/bot{token}/setWebhook", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            log.info("Telegram setWebhook → {} ({})", webhookUrl, resp);
        } catch (Exception ex) {
            log.error("Telegram setWebhook failed: {}", ex.getMessage());
        }
    }

    public void ensureMenuButton() {
        if (menuReady.get()) return;
        String token = tokenOrNull();
        String miniApp = properties.getMiniAppUrl();
        if (token == null || !StringUtils.hasText(miniApp) || !miniApp.toLowerCase().startsWith("https://")) {
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "menu_button", Map.of(
                            "type", "web_app",
                            "text", "Открыть Тайно",
                            "web_app", Map.of("url", miniApp)
                    )
            );
            restClient.post()
                    .uri("/bot{token}/setChatMenuButton", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            menuReady.set(true);
        } catch (Exception ex) {
            log.warn("Telegram setChatMenuButton failed: {}", ex.getMessage());
        }
    }

    public void ensureBotCommands() {
        String token = tokenOrNull();
        if (token == null) return;
        try {
            Map<String, Object> body = Map.of(
                    "commands", List.of(
                            Map.of("command", "start", "description", "Открыть Тайно")
                    )
            );
            restClient.post()
                    .uri("/bot{token}/setMyCommands", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Telegram setMyCommands failed: {}", ex.getMessage());
        }
    }

    public void ensureBotProfile() {
        String token = tokenOrNull();
        if (token == null) return;
        // Shown on the bot profile under «What can this bot do?» (max 512 chars).
        String description = """
                Тайно — анонимный обмен подарками в Telegram 🎁

                Правила:
                1) Пишешь желания — название, ссылка, фото. Имени и своего цвета нет.
                2) Ждёшь общий срок — чужие списки закрыты.
                3) Тебе выпадает один чужой список без имени.
                4) Даришь тихо до даты вручения.

                Нажми /start или кнопку «Открыть Тайно», чтобы запустить приложение
                """.trim();
        try {
            restClient.post()
                    .uri("/bot{token}/setMyDescription", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("description", description))
                    .retrieve()
                    .toBodilessEntity();
            restClient.post()
                    .uri("/bot{token}/setMyShortDescription", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "short_description",
                            "Анонимный обмен подарками: вишлисты без имён 🎁"
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Telegram setMyDescription failed: {}", ex.getMessage());
        }
    }

    private boolean send(long chatId, String text, Map<String, Object> replyMarkup) {
        String token = tokenOrNull();
        if (token == null) {
            log.warn("Telegram send skipped: bot token not configured");
            return false;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            body.put("disable_web_page_preview", true);
            if (replyMarkup != null) {
                body.put("reply_markup", replyMarkup);
            }
            Map<?, ?> resp = restClient.post()
                    .uri("/bot{token}/sendMessage", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            Object ok = resp != null ? resp.get("ok") : null;
            if (Boolean.FALSE.equals(ok)) {
                log.warn("Telegram sendMessage rejected: {}", resp);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Telegram sendMessage failed: {}", ex.getMessage());
            return false;
        }
    }

    private String tokenOrNull() {
        String token = properties.getTelegram().getBotToken();
        if (token == null || token.isBlank() || token.contains("ABC-DEF")) {
            return null;
        }
        return token;
    }
}

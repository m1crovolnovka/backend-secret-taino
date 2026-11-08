package com.taino.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates Telegram Mini App initData per
 * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
 */
@Component
public class InitDataValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;

    public InitDataValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TelegramUserPayload validate(String initData, String botToken, long maxAgeSeconds) {
        if (initData == null || initData.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_REQUIRED", "Нет данных авторизации Telegram");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "BOT_TOKEN_MISSING", "Бот не настроен");
        }

        Map<String, String> fields = parse(initData);
        String hash = fields.remove("hash");
        if (hash == null || hash.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "Подпись initData отсутствует");
        }

        String authDateRaw = fields.get("auth_date");
        if (authDateRaw == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "auth_date отсутствует");
        }
        long authDate;
        try {
            authDate = Long.parseLong(authDateRaw);
        } catch (NumberFormatException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "auth_date некорректен");
        }
        long age = Instant.now().getEpochSecond() - authDate;
        if (age < 0 || age > maxAgeSeconds) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_EXPIRED", "Данные авторизации устарели");
        }

        String dataCheckString = fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
        byte[] calculated = hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
        String calculatedHex = toHex(calculated);

        byte[] expected = calculatedHex.getBytes(StandardCharsets.UTF_8);
        byte[] provided = hash.toLowerCase().getBytes(StandardCharsets.UTF_8);
        // isEqual throws if lengths differ — treat that as invalid signature, not 500
        if (expected.length != provided.length || !MessageDigest.isEqual(expected, provided)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "Подпись initData неверна");
        }

        String userJson = fields.get("user");
        if (userJson == null || userJson.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "user отсутствует в initData");
        }

        try {
            JsonNode node = objectMapper.readTree(userJson);
            long id = node.path("id").asLong(0);
            if (id <= 0) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "user.id отсутствует");
            }
            String firstName = textOrNull(node, "first_name");
            if (firstName == null || firstName.isBlank()) {
                firstName = "User";
            }
            return new TelegramUserPayload(
                    id,
                    textOrNull(node, "username"),
                    firstName,
                    textOrNull(node, "last_name"),
                    textOrNull(node, "language_code")
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INIT_DATA_INVALID", "Не удалось разобрать user");
        }
    }

    static Map<String, String> parse(String initData) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            return mac.doFinal(data);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC failure", ex);
        }
    }

    static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

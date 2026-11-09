package com.taino.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taino.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitDataValidatorTest {

    private static final String BOT_TOKEN = "123456:ABC-DEF";

    private InitDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InitDataValidator(new ObjectMapper());
    }

    @Test
    void acceptsValidInitData() {
        long authDate = Instant.now().getEpochSecond();
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "user", "{\"id\":42,\"first_name\":\"Denis\",\"username\":\"denis\",\"language_code\":\"ru\"}"
        ));

        InitDataResult result = validator.validate(initData, BOT_TOKEN, 3600);
        TelegramUserPayload user = result.user();

        assertEquals(42L, user.id());
        assertEquals("Denis", user.firstName());
        assertEquals("denis", user.username());
        assertEquals("ru", user.languageCode());
        assertEquals(null, result.startParam());
    }

    @Test
    void readsStartParamFromInitData() {
        long authDate = Instant.now().getEpochSecond();
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "start_param", "room_ABC123",
                "user", "{\"id\":42,\"first_name\":\"Denis\"}"
        ));

        InitDataResult result = validator.validate(initData, BOT_TOKEN, 3600);
        assertEquals("room_ABC123", result.startParam());
        assertEquals(42L, result.user().id());
    }

    @Test
    void rejectsTamperedHash() {
        long authDate = Instant.now().getEpochSecond();
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "user", "{\"id\":1,\"first_name\":\"A\"}"
        )) + "ff";

        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validate(initData, BOT_TOKEN, 3600));
        assertEquals("INIT_DATA_INVALID", ex.getCode());
    }

    @Test
    void rejectsExpiredAuthDate() {
        long authDate = Instant.now().getEpochSecond() - 7200;
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "user", "{\"id\":1,\"first_name\":\"A\"}"
        ));

        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validate(initData, BOT_TOKEN, 3600));
        assertEquals("INIT_DATA_EXPIRED", ex.getCode());
    }

    @Test
    void rejectsMissingUser() {
        long authDate = Instant.now().getEpochSecond();
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "AAE"
        ));

        ApiException ex = assertThrows(ApiException.class,
                () -> validator.validate(initData, BOT_TOKEN, 3600));
        assertTrue(ex.getCode().startsWith("INIT_DATA"));
    }

    @Test
    void acceptsUserWithoutUsername() {
        long authDate = Instant.now().getEpochSecond();
        String initData = signedInitData(Map.of(
                "auth_date", String.valueOf(authDate),
                "user", "{\"id\":7,\"first_name\":\"Anon\"}"
        ));

        TelegramUserPayload user = validator.validate(initData, BOT_TOKEN, 3600).user();
        assertEquals(7L, user.id());
        assertEquals(null, user.username());
        assertEquals("Anon", user.firstName());
    }

    static String signedInitData(Map<String, String> fields) {
        return signedInitData(fields, BOT_TOKEN);
    }

    static String signedInitData(Map<String, String> fields, String botToken) {
        String dataCheckString = fields.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
        String hash = toHex(hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        StringBuilder sb = new StringBuilder();
        fields.forEach((k, v) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        sb.append("&hash=").append(hash);
        return sb.toString();
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

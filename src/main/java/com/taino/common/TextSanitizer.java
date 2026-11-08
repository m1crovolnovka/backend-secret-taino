package com.taino.common;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextSanitizer {

    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private TextSanitizer() {
    }

    public static String plain(String input, int maxLen) {
        if (input == null) {
            return null;
        }
        String cleaned = TAGS.matcher(input).replaceAll("");
        cleaned = CONTROL.matcher(cleaned).replaceAll("");
        cleaned = cleaned.trim();
        if (cleaned.length() > maxLen) {
            cleaned = cleaned.substring(0, maxLen);
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    public static String requirePlain(String input, int maxLen, String field) {
        String cleaned = plain(input, maxLen);
        if (!StringUtils.hasText(cleaned)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", field + " обязателен");
        }
        return cleaned;
    }

    public static String httpUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > 2000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Ссылка слишком длинная");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("://") && !lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Разрешены только http/https");
        }
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            value = "https://" + value;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Некорректная ссылка");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Разрешены только http/https");
        }
        if (!StringUtils.hasText(uri.getHost())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Некорректная ссылка");
        }
        return value;
    }
}

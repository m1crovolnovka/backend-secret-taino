package com.taino.auth;

public record TelegramUserPayload(
        long id,
        String username,
        String firstName,
        String lastName,
        String languageCode
) {
}

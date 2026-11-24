package com.taino.auth;

public record AuthenticatedUser(Long userId, Long telegramId, boolean staff) {

    public static AuthenticatedUser telegram(Long userId, Long telegramId) {
        return new AuthenticatedUser(userId, telegramId, false);
    }

    public static AuthenticatedUser staff(Long staffId) {
        return new AuthenticatedUser(staffId, null, true);
    }
}

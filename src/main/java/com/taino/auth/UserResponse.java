package com.taino.auth;

public record UserResponse(
        Long id,
        Long telegramId,
        String username,
        String firstName,
        String lastName
) {
}

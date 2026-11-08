package com.taino.auth;

public record AuthResponse(
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}

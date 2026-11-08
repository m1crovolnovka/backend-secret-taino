package com.taino.auth;

import jakarta.validation.constraints.NotBlank;

public record TelegramAuthRequest(
        @NotBlank(message = "initData обязателен")
        String initData
) {
}

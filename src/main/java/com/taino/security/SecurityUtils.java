package com.taino.security;

import com.taino.auth.AuthenticatedUser;
import com.taino.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUser() {
        AuthenticatedUser user = principal();
        if (user.staff()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_ONLY_PANEL", "Этот API для Mini App");
        }
        return user;
    }

    public static AuthenticatedUser currentStaff() {
        AuthenticatedUser user = principal();
        if (!user.staff()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "STAFF_REQUIRED", "Нужен вход в панель аналитики");
        }
        return user;
    }

    private static AuthenticatedUser principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Требуется авторизация");
        }
        return user;
    }
}

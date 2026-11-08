package com.taino.auth;

import com.taino.common.ApiException;
import com.taino.config.TainoProperties;
import com.taino.user.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(TainoProperties properties) {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = properties.getJwt().getTtlSeconds();
    }

    public String issue(UserEntity user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("tid", user.getTelegramId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            Long telegramId = claims.get("tid", Number.class).longValue();
            return new AuthenticatedUser(userId, telegramId);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Сессия недействительна");
        }
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}

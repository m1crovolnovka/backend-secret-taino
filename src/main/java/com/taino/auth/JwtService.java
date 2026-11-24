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
                .claim("typ", "user")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String issueStaff(Long staffId, String login) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(staffId))
                .claim("typ", "staff")
                .claim("login", login)
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
            Long id = Long.valueOf(claims.getSubject());
            String typ = claims.get("typ", String.class);
            if ("staff".equalsIgnoreCase(typ)) {
                return AuthenticatedUser.staff(id);
            }
            Number tid = claims.get("tid", Number.class);
            if (tid == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Сессия недействительна");
            }
            return AuthenticatedUser.telegram(id, tid.longValue());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Сессия недействительна");
        }
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }
}

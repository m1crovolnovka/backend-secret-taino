package com.taino.auth;

import com.taino.common.ApiException;
import com.taino.config.TainoProperties;
import com.taino.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        TainoProperties props = new TainoProperties();
        props.getJwt().setSecret("test-secret-key-at-least-32-characters!!");
        props.getJwt().setTtlSeconds(3600);
        jwtService = new JwtService(props);
    }

    @Test
    void issuesAndParsesToken() {
        UserEntity user = new UserEntity();
        user.setTelegramId(99L);
        // id is assigned by JPA; set via reflection-free path using parse after issue needs id
        // Use a stub by saving id through package - set via issue requiring getId()
        setId(user, 5L);

        String token = jwtService.issue(user);
        AuthenticatedUser parsed = jwtService.parse(token);

        assertEquals(5L, parsed.userId());
        assertEquals(99L, parsed.telegramId());
    }

    @Test
    void rejectsGarbageToken() {
        assertThrows(ApiException.class, () -> jwtService.parse("not.a.jwt"));
    }

    private static void setId(UserEntity user, Long id) {
        try {
            var field = UserEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

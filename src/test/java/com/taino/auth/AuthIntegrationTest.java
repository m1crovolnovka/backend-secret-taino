package com.taino.auth;

import com.taino.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taino")
            .withUsername("taino")
            .withPassword("taino");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Test
    void loginCreatesUserAndReturnsJwt() throws Exception {
        String initData = InitDataValidatorTest.signedInitData(Map.of(
                "auth_date", String.valueOf(Instant.now().getEpochSecond()),
                "user", "{\"id\":1001,\"first_name\":\"Тест\",\"username\":\"tester\"}"
        ), "123456:TEST-TOKEN");

        MvcResult result = mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.telegramId").value(1001))
                .andExpect(jsonPath("$.user.username").value("tester"))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramId").value(1001));

        assertThat(userRepository.findByTelegramId(1001L)).isPresent();

        // username update on re-login
        String initData2 = InitDataValidatorTest.signedInitData(Map.of(
                "auth_date", String.valueOf(Instant.now().getEpochSecond()),
                "user", "{\"id\":1001,\"first_name\":\"Тест\",\"username\":\"tester2\"}"
        ), "123456:TEST-TOKEN");
        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"" + escapeJson(initData2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("tester2"));
    }

    @Test
    void rejectsBadInitData() throws Exception {
        mockMvc.perform(post("/api/auth/telegram")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initData\":\"auth_date=1&hash=deadbeef\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

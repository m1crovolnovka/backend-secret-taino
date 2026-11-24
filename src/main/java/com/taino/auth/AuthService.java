package com.taino.auth;

import com.taino.analytics.AnalyticsEventType;
import com.taino.analytics.AnalyticsIngestService;
import com.taino.audit.AuditService;
import com.taino.bot.BotGreetingService;
import com.taino.config.TainoProperties;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final InitDataValidator initDataValidator;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TainoProperties properties;
    private final BotGreetingService botGreetingService;
    private final AuditService auditService;
    private final AnalyticsIngestService analyticsIngestService;

    public AuthService(
            InitDataValidator initDataValidator,
            UserRepository userRepository,
            JwtService jwtService,
            TainoProperties properties,
            BotGreetingService botGreetingService,
            AuditService auditService,
            AnalyticsIngestService analyticsIngestService
    ) {
        this.initDataValidator = initDataValidator;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.properties = properties;
        this.botGreetingService = botGreetingService;
        this.auditService = auditService;
        this.analyticsIngestService = analyticsIngestService;
    }

    @Transactional
    public AuthResponse authenticate(String initData) {
        InitDataResult validated = initDataValidator.validate(
                initData,
                properties.getTelegram().getBotToken(),
                properties.getTelegram().getInitDataMaxAgeSeconds()
        );
        TelegramUserPayload tg = validated.user();

        UserEntity user = userRepository.findByTelegramId(tg.id())
                .orElseGet(UserEntity::new);
        boolean firstVisit = user.getId() == null;
        boolean neverGreeted = user.getBotGreetedAt() == null;
        String startParam = validated.startParam();

        user.setTelegramId(tg.id());
        user.setUsername(tg.username());
        user.setFirstName(tg.firstName());
        user.setLastName(tg.lastName());
        user.setLanguageCode(tg.languageCode());
        user = userRepository.save(user);

        // startapp opens Mini App without /start in chat — write a bot message so history exists.
        boolean deepLink = StringUtils.hasText(startParam);
        if (deepLink || firstVisit || neverGreeted) {
            try {
                if (botGreetingService.greet(tg.id(), startParam)) {
                    user.setBotGreetedAt(Instant.now());
                    userRepository.save(user);
                }
            } catch (Exception ex) {
                log.warn("Bot greeting failed for tg {}: {}", tg.id(), ex.getMessage());
            }
        }

        String token = jwtService.issue(user);
        auditService.record(user.getId(), null, firstVisit ? "AUTH_REGISTER" : "AUTH_LOGIN",
                Map.of("deepLink", deepLink));
        try {
            analyticsIngestService.attributeFromStartParam(user.getId(), startParam);
            analyticsIngestService.emit(
                    firstVisit ? AnalyticsEventType.USER_REGISTER : AnalyticsEventType.USER_LOGIN,
                    user.getId(),
                    startParam,
                    Map.of("deepLink", deepLink)
            );
        } catch (Exception ex) {
            log.warn("Analytics ingest failed for user {}: {}", user.getId(), ex.getMessage());
        }
        return new AuthResponse(
                token,
                jwtService.ttlSeconds(),
                new UserResponse(
                        user.getId(),
                        user.getTelegramId(),
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName()
                )
        );
    }
}

package com.taino.auth;

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

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final InitDataValidator initDataValidator;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TainoProperties properties;
    private final BotGreetingService botGreetingService;

    public AuthService(
            InitDataValidator initDataValidator,
            UserRepository userRepository,
            JwtService jwtService,
            TainoProperties properties,
            BotGreetingService botGreetingService
    ) {
        this.initDataValidator = initDataValidator;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.properties = properties;
        this.botGreetingService = botGreetingService;
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

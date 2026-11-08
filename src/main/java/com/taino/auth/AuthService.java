package com.taino.auth;

import com.taino.config.TainoProperties;
import com.taino.user.UserEntity;
import com.taino.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final InitDataValidator initDataValidator;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TainoProperties properties;

    public AuthService(
            InitDataValidator initDataValidator,
            UserRepository userRepository,
            JwtService jwtService,
            TainoProperties properties
    ) {
        this.initDataValidator = initDataValidator;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse authenticate(String initData) {
        TelegramUserPayload tg = initDataValidator.validate(
                initData,
                properties.getTelegram().getBotToken(),
                properties.getTelegram().getInitDataMaxAgeSeconds()
        );

        UserEntity user = userRepository.findByTelegramId(tg.id())
                .orElseGet(UserEntity::new);

        user.setTelegramId(tg.id());
        user.setUsername(tg.username());
        user.setFirstName(tg.firstName());
        user.setLastName(tg.lastName());
        user.setLanguageCode(tg.languageCode());
        user = userRepository.save(user);

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

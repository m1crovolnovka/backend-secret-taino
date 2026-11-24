package com.taino.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StaffAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffAccountBootstrap.class);

    private final StaffAccountRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountBootstrap(StaffAccountRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (staffRepository.existsByLoginIgnoreCase("admin")) {
            return;
        }
        StaffAccountEntity admin = new StaffAccountEntity();
        admin.setLogin("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setMustChangePassword(true);
        staffRepository.save(admin);
        log.info("Seeded analytics staff account login=admin (change password on first login)");
    }
}

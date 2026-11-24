package com.taino.analytics;

import com.taino.auth.AuthenticatedUser;
import com.taino.auth.JwtService;
import com.taino.common.ApiException;
import com.taino.common.TextSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffAuthService {

    private final StaffAccountRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public StaffAuthService(
            StaffAccountRepository staffRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.StaffAuthResponse login(AnalyticsDtos.StaffLoginRequest req) {
        StaffAccountEntity staff = staffRepository.findByLoginIgnoreCase(req.login().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Неверный логин или пароль"));
        if (!passwordEncoder.matches(req.password(), staff.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Неверный логин или пароль");
        }
        return tokenResponse(staff);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.StaffMe me(Long staffId) {
        StaffAccountEntity staff = require(staffId);
        return toMe(staff);
    }

    @Transactional
    public AnalyticsDtos.StaffAuthResponse changePassword(Long staffId, AnalyticsDtos.StaffChangePasswordRequest req) {
        StaffAccountEntity staff = require(staffId);
        if (!passwordEncoder.matches(req.currentPassword(), staff.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_PASSWORD", "Текущий пароль неверный");
        }
        String nextLogin = StringUtils.hasText(req.newLogin())
                ? TextSanitizer.requirePlain(req.newLogin(), 64, "Логин")
                : staff.getLogin();
        if (!nextLogin.equalsIgnoreCase(staff.getLogin()) && staffRepository.existsByLoginIgnoreCase(nextLogin)) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_TAKEN", "Такой логин уже занят");
        }
        if ("admin".equalsIgnoreCase(req.newPassword()) || req.newPassword().equalsIgnoreCase(nextLogin)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "Пароль слишком простой");
        }
        staff.setLogin(nextLogin);
        staff.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        staff.setMustChangePassword(false);
        staffRepository.save(staff);
        return tokenResponse(staff);
    }

    private StaffAccountEntity require(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Сессия недействительна"));
    }

    private AnalyticsDtos.StaffAuthResponse tokenResponse(StaffAccountEntity staff) {
        String token = jwtService.issueStaff(staff.getId(), staff.getLogin());
        return new AnalyticsDtos.StaffAuthResponse(token, jwtService.ttlSeconds(), toMe(staff));
    }

    private static AnalyticsDtos.StaffMe toMe(StaffAccountEntity staff) {
        return new AnalyticsDtos.StaffMe(staff.getId(), staff.getLogin(), staff.isMustChangePassword());
    }
}

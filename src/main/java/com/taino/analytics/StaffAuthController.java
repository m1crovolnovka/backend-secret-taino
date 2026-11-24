package com.taino.analytics;

import com.taino.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/staff")
public class StaffAuthController {

    private final StaffAuthService staffAuthService;

    public StaffAuthController(StaffAuthService staffAuthService) {
        this.staffAuthService = staffAuthService;
    }

    @PostMapping("/login")
    public AnalyticsDtos.StaffAuthResponse login(@Valid @RequestBody AnalyticsDtos.StaffLoginRequest request) {
        return staffAuthService.login(request);
    }

    @GetMapping("/me")
    public AnalyticsDtos.StaffMe me() {
        return staffAuthService.me(SecurityUtils.currentStaff().userId());
    }

    @PostMapping("/change-password")
    public AnalyticsDtos.StaffAuthResponse changePassword(
            @Valid @RequestBody AnalyticsDtos.StaffChangePasswordRequest request
    ) {
        return staffAuthService.changePassword(SecurityUtils.currentStaff().userId(), request);
    }
}

package com.gativah.admin.auth.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.dto.MfaEnableRequest;
import com.gativah.admin.auth.dto.MfaStartResponse;
import com.gativah.admin.auth.dto.MfaStatusResponse;
import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.auth.service.AdminAuthService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Self-service TOTP MFA enrollment for the signed-in operator. */
@RestController
@RequestMapping("/api/v1/admin/me/mfa")
@PreAuthorize("isAuthenticated()")
public class AdminMfaController {

    private final AdminAuthService authService;

    public AdminMfaController(AdminAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public MfaStatusResponse status(@AuthenticationPrincipal AdminPrincipal principal) {
        return authService.mfaStatus(principal.id());
    }

    @PostMapping("/start")
    public MfaStartResponse start(@AuthenticationPrincipal AdminPrincipal principal) {
        return authService.startMfa(principal.id());
    }

    @PostMapping("/enable")
    public MfaStatusResponse enable(@AuthenticationPrincipal AdminPrincipal principal,
                                    @Valid @RequestBody MfaEnableRequest req) {
        return authService.enableMfa(principal.id(), req);
    }
}

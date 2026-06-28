package com.gativah.admin.auth.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.dto.AdminMeResponse;
import com.gativah.admin.auth.dto.AuthResponse;
import com.gativah.admin.auth.dto.LoginRequest;
import com.gativah.admin.auth.dto.MfaRequest;
import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.auth.service.AdminAuthService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff sign-in (password → optional TOTP) and the current-operator endpoint. */
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/verify-mfa")
    public AuthResponse verifyMfa(@Valid @RequestBody MfaRequest req) {
        return authService.verifyMfa(req);
    }

    @GetMapping("/me")
    public AdminMeResponse me(@AuthenticationPrincipal AdminPrincipal principal, Authentication auth) {
        return new AdminMeResponse(
                principal.id(), principal.email(), principal.name(), principal.role().name(),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}

package com.gativah.admin.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.dto.AdminMeResponse;
import com.gativah.admin.auth.dto.AuthResponse;
import com.gativah.admin.auth.dto.LoginRequest;
import com.gativah.admin.auth.dto.MfaRequest;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.auth.security.AdminJwtService;
import com.gativah.admin.auth.security.TotpService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff authentication: password + (when enrolled) TOTP MFA, issuing a
 * short-lived admin JWT. Invalid credentials, disabled accounts, and bad MFA
 * codes all surface as 401 to avoid account enumeration.
 */
@Service
public class AdminAuthService {

    private final AdminUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final AdminJwtService jwtService;
    private final AuditService auditService;

    public AdminAuthService(AdminUserRepository repo, PasswordEncoder passwordEncoder,
                            TotpService totpService, AdminJwtService jwtService, AuditService auditService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    /** Step 1: verify password. If MFA is enrolled, return a challenge (no token). */
    @Transactional
    public AuthResponse login(LoginRequest req) {
        AdminUser user = authenticate(req.email(), req.password());
        if (user.isMfaEnrolled()) {
            return AuthResponse.mfaChallenge();
        }
        return issue(user);
    }

    /** Step 2: verify password again + TOTP code, then issue the token. */
    @Transactional
    public AuthResponse verifyMfa(MfaRequest req) {
        AdminUser user = authenticate(req.email(), req.password());
        if (!user.isMfaEnrolled() || user.getMfaSecret() == null
                || !totpService.verify(user.getMfaSecret(), req.code())) {
            throw unauthorized();
        }
        return issue(user);
    }

    private AdminUser authenticate(String email, String password) {
        AdminUser user = repo.findByEmailIgnoreCase(email).orElseThrow(this::unauthorized);
        if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw unauthorized();
        }
        return user;
    }

    private AuthResponse issue(AdminUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        repo.save(user);
        List<String> authorities = user.getRole().authorities();
        String token = jwtService.generate(user, authorities);
        auditService.record(user.getId(), "LOGIN", "Signed in");
        AdminMeResponse me = new AdminMeResponse(
                user.getId(), user.getEmail(), user.getName(), user.getRole().name(), authorities);
        return AuthResponse.issued(token, jwtService.expirationMs(), me);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}

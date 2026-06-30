package com.gativah.admin.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.dto.AdminMeResponse;
import com.gativah.admin.auth.dto.AuthResponse;
import com.gativah.admin.auth.dto.LoginRequest;
import com.gativah.admin.auth.dto.MfaEnableRequest;
import com.gativah.admin.auth.dto.MfaRequest;
import com.gativah.admin.auth.dto.MfaStartResponse;
import com.gativah.admin.auth.dto.MfaStatusResponse;
import com.gativah.admin.auth.model.AdminSession;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminSessionRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.auth.security.AdminJwtService;
import com.gativah.admin.auth.security.TotpService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * Staff authentication: password + (when enrolled) TOTP MFA, issuing a
 * short-lived admin JWT. Invalid credentials, disabled accounts, and bad MFA
 * codes all surface as 401 to avoid account enumeration.
 */
@Service
public class AdminAuthService {

    private final AdminUserRepository repo;
    private final AdminSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final AdminJwtService jwtService;
    private final AuditService auditService;

    public AdminAuthService(AdminUserRepository repo, AdminSessionRepository sessions, PasswordEncoder passwordEncoder,
                            TotpService totpService, AdminJwtService jwtService, AuditService auditService) {
        this.repo = repo;
        this.sessions = sessions;
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

    /**
     * Sliding session: re-issue a fresh token for the still-authenticated operator,
     * reusing the same session (jti) so the session list + per-session revoke stay
     * accurate. The JWT filter has already validated the caller (active + token
     * version + session not revoked) before this runs.
     */
    @Transactional
    public AuthResponse refresh(Long adminId, String jti) {
        AdminUser user = requireAdmin(adminId);
        if (!user.isActive()) {
            throw unauthorized();
        }
        List<String> authorities = user.permissionCodes();
        String token = jwtService.generate(user, authorities, jti);
        AdminMeResponse me = new AdminMeResponse(
                user.getId(), user.getEmail(), user.getName(), user.roleNames(), authorities);
        return AuthResponse.issued(token, jwtService.expirationMs(), me);
    }

    // ── MFA enrollment (authenticated operator) ───────────────
    @Transactional(readOnly = true)
    public MfaStatusResponse mfaStatus(Long adminId) {
        return new MfaStatusResponse(requireAdmin(adminId).isMfaEnrolled());
    }

    /** Generate + persist a fresh secret (enrollment stays pending until confirmed). */
    @Transactional
    public MfaStartResponse startMfa(Long adminId) {
        AdminUser u = requireAdmin(adminId);
        String secret = totpService.generateSecret();
        u.setMfaSecret(secret);
        repo.save(u);
        return new MfaStartResponse(secret, totpService.provisioningUri(secret, u.getEmail(), "Gativah Admin"),
                u.isMfaEnrolled());
    }

    /** Confirm a code against the pending secret and switch MFA on. */
    @Transactional
    public MfaStatusResponse enableMfa(Long adminId, MfaEnableRequest req) {
        AdminUser u = requireAdmin(adminId);
        if (u.getMfaSecret() == null || !totpService.verify(u.getMfaSecret(), req.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
        }
        u.setMfaEnrolled(true);
        repo.save(u);
        auditService.record(adminId, "MFA_ENABLE", "Enabled TOTP MFA");
        return new MfaStatusResponse(true);
    }

    private AdminUser requireAdmin(Long adminId) {
        return repo.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));
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
        List<String> authorities = user.permissionCodes();
        String jti = UUID.randomUUID().toString();
        AdminSession session = new AdminSession();
        session.setAdminUserId(user.getId());
        session.setJti(jti);
        session.setIp(currentIp());
        session.setUserAgent(currentUserAgent());
        sessions.save(session);
        String token = jwtService.generate(user, authorities, jti);
        auditService.record(user.getId(), "LOGIN", "Signed in");
        AdminMeResponse me = new AdminMeResponse(
                user.getId(), user.getEmail(), user.getName(), user.roleNames(), authorities);
        return AuthResponse.issued(token, jwtService.expirationMs(), me);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    private static String currentIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private static String currentUserAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String ua = req.getHeader("User-Agent");
        return ua != null && ua.length() > 400 ? ua.substring(0, 400) : ua;
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

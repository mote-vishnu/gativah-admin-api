package com.gativah.admin.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.dto.AuthResponse;
import com.gativah.admin.auth.dto.LoginRequest;
import com.gativah.admin.auth.dto.MfaRequest;
import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.auth.security.AdminJwtService;
import com.gativah.admin.auth.security.TotpService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock AdminUserRepository repo;
    @Mock PasswordEncoder encoder;
    @Mock TotpService totp;
    @Mock AdminJwtService jwt;
    @Mock AuditService audit;

    AdminAuthService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuthService(repo, encoder, totp, jwt, audit);
    }

    private AdminUser user(boolean active, boolean mfa) {
        AdminRole moderator = new AdminRole();
        moderator.setName("MODERATOR");
        AdminUser u = new AdminUser();
        u.setId(7L);
        u.setEmail("dev@gativah.com");
        u.setName("Dev K.");
        u.setRoles(Set.of(moderator));
        u.setStatus(active ? AdminUser.STATUS_ACTIVE : AdminUser.STATUS_DISABLED);
        u.setPasswordHash("hash");
        u.setMfaEnrolled(mfa);
        if (mfa) {
            u.setMfaSecret("GEZDGNBVGY3TQOJQ");
        }
        return u;
    }

    @Test
    void login_without_mfa_issues_a_token() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(true, false)));
        when(encoder.matches("pw", "hash")).thenReturn(true);
        when(jwt.generate(any(), any())).thenReturn("tok");
        when(jwt.expirationMs()).thenReturn(1_800_000L);

        AuthResponse res = service.login(new LoginRequest("dev@gativah.com", "pw"));

        assertThat(res.mfaRequired()).isFalse();
        assertThat(res.token()).isEqualTo("tok");
        assertThat(res.user().roles()).containsExactly("MODERATOR");
        verify(audit).record(eq(7L), eq("LOGIN"), anyString());
    }

    @Test
    void login_with_mfa_enrolled_returns_a_challenge_and_no_token() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(true, true)));
        when(encoder.matches("pw", "hash")).thenReturn(true);

        AuthResponse res = service.login(new LoginRequest("dev@gativah.com", "pw"));

        assertThat(res.mfaRequired()).isTrue();
        assertThat(res.token()).isNull();
        verify(jwt, never()).generate(any(), any());
    }

    @Test
    void bad_password_is_401() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(true, false)));
        when(encoder.matches("pw", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("dev@gativah.com", "pw")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void disabled_account_is_401() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(false, false)));

        assertThatThrownBy(() -> service.login(new LoginRequest("dev@gativah.com", "pw")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void unknown_email_is_401() {
        when(repo.findByEmailIgnoreCase("nope@gativah.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nope@gativah.com", "pw")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void verify_mfa_with_good_code_issues_a_token() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(true, true)));
        when(encoder.matches("pw", "hash")).thenReturn(true);
        when(totp.verify(anyString(), eq("123456"))).thenReturn(true);
        when(jwt.generate(any(), any())).thenReturn("tok");
        when(jwt.expirationMs()).thenReturn(1_800_000L);

        AuthResponse res = service.verifyMfa(new MfaRequest("dev@gativah.com", "pw", "123456"));

        assertThat(res.token()).isEqualTo("tok");
        assertThat(res.mfaRequired()).isFalse();
    }

    @Test
    void verify_mfa_with_bad_code_is_401() {
        when(repo.findByEmailIgnoreCase("dev@gativah.com")).thenReturn(Optional.of(user(true, true)));
        when(encoder.matches("pw", "hash")).thenReturn(true);
        when(totp.verify(anyString(), eq("000000"))).thenReturn(false);

        assertThatThrownBy(() -> service.verifyMfa(new MfaRequest("dev@gativah.com", "pw", "000000")))
                .isInstanceOf(ResponseStatusException.class);
    }
}

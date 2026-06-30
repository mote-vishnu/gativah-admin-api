package com.gativah.admin.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminRoleRepository;
import com.gativah.admin.auth.repo.AdminSessionRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.staff.dto.InviteStaffRequest;
import com.gativah.admin.staff.dto.StaffRow;
import com.gativah.admin.staff.dto.UpdateStaffRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StaffServiceImplTest {

    @Mock AdminUserRepository repo;
    @Mock AdminRoleRepository roleRepo;
    @Mock AdminSessionRepository sessionRepo;
    @Mock PasswordEncoder encoder;
    @Mock AuditService audit;

    StaffServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StaffServiceImpl(repo, roleRepo, sessionRepo, encoder, audit);
    }

    private static AdminRole role(Long id, String name) {
        AdminRole r = new AdminRole();
        r.setId(id);
        r.setName(name);
        return r;
    }

    @Test
    void invite_creates_hashed_active_admin_with_roles_and_audits() {
        when(repo.existsByEmailIgnoreCase("new@gativah.com")).thenReturn(false);
        when(roleRepo.findById(3L)).thenReturn(Optional.of(role(3L, "MODERATOR")));
        when(encoder.encode("a-strong-pass")).thenReturn("hash");
        when(repo.save(any(AdminUser.class))).thenAnswer(inv -> {
            AdminUser u = inv.getArgument(0);
            u.setId(9L);
            return u;
        });

        StaffRow row = service.invite(1L,
                new InviteStaffRequest("new@gativah.com", "New Mod", List.of(3L), "a-strong-pass"));

        assertThat(row.id()).isEqualTo(9L);
        assertThat(row.roles()).containsExactly("MODERATOR");
        assertThat(row.status()).isEqualTo(AdminUser.STATUS_ACTIVE);
        verify(audit).record(any(), eq("STAFF_INVITE"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void invite_rejects_duplicate_email() {
        when(repo.existsByEmailIgnoreCase("dup@gativah.com")).thenReturn(true);
        assertThatThrownBy(() -> service.invite(1L,
                new InviteStaffRequest("dup@gativah.com", "Dup", List.of(1L), "a-strong-pass")))
                .isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void invite_rejects_unknown_role() {
        when(repo.existsByEmailIgnoreCase("new@gativah.com")).thenReturn(false);
        when(roleRepo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.invite(1L,
                new InviteStaffRequest("new@gativah.com", "New", List.of(99L), "a-strong-pass")))
                .isInstanceOf(ResponseStatusException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_changes_status_only() {
        AdminUser u = new AdminUser();
        u.setId(5L);
        u.setStatus(AdminUser.STATUS_ACTIVE);
        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(repo.save(any(AdminUser.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffRow row = service.update(1L, 5L, new UpdateStaffRequest(AdminUser.STATUS_DISABLED));

        assertThat(row.status()).isEqualTo(AdminUser.STATUS_DISABLED);
    }

    @Test
    void disabling_bumps_token_version_for_forced_logout() {
        AdminUser u = new AdminUser();
        u.setId(5L);
        u.setStatus(AdminUser.STATUS_ACTIVE);
        u.setTokenVersion(3);
        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(repo.save(any(AdminUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(1L, 5L, new UpdateStaffRequest(AdminUser.STATUS_DISABLED));

        assertThat(u.getTokenVersion()).isEqualTo(4);
    }

    @Test
    void set_roles_replaces_the_assigned_roles() {
        AdminUser u = new AdminUser();
        u.setId(5L);
        when(repo.findById(5L)).thenReturn(Optional.of(u));
        when(roleRepo.findById(2L)).thenReturn(Optional.of(role(2L, "FINANCE_ANALYST")));
        when(repo.save(any(AdminUser.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffRow row = service.setRoles(1L, 5L, List.of(2L));

        assertThat(row.roles()).containsExactly("FINANCE_ANALYST");
        verify(audit).record(any(), eq("STAFF_SET_ROLES"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void update_missing_is_404() {
        when(repo.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, 404L, new UpdateStaffRequest(null)))
                .isInstanceOf(ResponseStatusException.class);
    }
}

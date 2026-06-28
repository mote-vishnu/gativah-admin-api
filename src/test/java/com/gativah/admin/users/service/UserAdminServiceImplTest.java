package com.gativah.admin.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.users.dto.BanRequest;
import com.gativah.admin.users.dto.SuspendRequest;
import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.query.UsersQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceImplTest {

    @Mock UsersQuery query;
    @Mock PacegritInternalClient internal;
    @Mock AuditService audit;

    UserAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAdminServiceImpl(query, internal, audit);
    }

    private UserDetail user(Long id) {
        return new UserDetail(id, "alex", "alex@x.com", "Alex", "K", null, true,
                "ACTIVE", null, null, null, LocalDateTime.now(), null, List.of());
    }

    @Test
    void list_wraps_query_into_a_like_pattern() {
        service.list("ale", null, Pageable.ofSize(20));
        verify(query).search(eq("%ale%"), isNull(), any(Pageable.class));
    }

    @Test
    void list_passes_status_list_through() {
        service.list("  ", List.of("ACTIVE", "SUSPENDED"), Pageable.ofSize(20));
        verify(query).search(isNull(), eq(List.of("ACTIVE", "SUSPENDED")), any(Pageable.class));
    }

    @Test
    void detail_missing_is_404() {
        when(query.detail(404L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(404L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void suspend_calls_hook_with_future_date_and_audits() {
        when(query.detail(7L)).thenReturn(user(7L));

        service.suspend(1L, 7L, new SuspendRequest("spam", 3));

        ArgumentCaptor<LocalDateTime> until = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(internal).suspendUser(eq(1L), eq(7L), eq("spam"), until.capture());
        assertThat(until.getValue()).isAfter(LocalDateTime.now());
        verify(audit).record(eq(1L), eq("USER_SUSPEND"), eq("USER"), eq("7"), anyString(), any(), any());
    }

    @Test
    void ban_calls_hook_and_audits() {
        when(query.detail(7L)).thenReturn(user(7L));

        service.ban(1L, 7L, new BanRequest("fraud"));

        verify(internal).banUser(1L, 7L, "fraud");
        verify(audit).record(eq(1L), eq("USER_BAN"), eq("USER"), eq("7"), anyString(), any(), any());
    }

    @Test
    void reinstate_calls_hook_and_audits() {
        when(query.detail(7L)).thenReturn(user(7L));

        service.reinstate(1L, 7L);

        verify(internal).reinstateUser(1L, 7L);
        verify(audit).record(eq(1L), eq("USER_REINSTATE"), eq("USER"), eq("7"), anyString(), any(), any());
    }

    @Test
    void action_on_missing_user_is_404_and_skips_hook() {
        when(query.detail(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.ban(1L, 404L, new BanRequest("x")))
                .isInstanceOf(ResponseStatusException.class);
        verify(internal, never()).banUser(any(), any(), any());
    }
}

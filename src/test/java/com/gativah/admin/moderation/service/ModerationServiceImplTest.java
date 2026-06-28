package com.gativah.admin.moderation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.moderation.dto.AppealResolveRequest;
import com.gativah.admin.moderation.dto.ResolveAction;
import com.gativah.admin.moderation.dto.ResolveRequest;
import com.gativah.admin.moderation.dto.ResolveResponse;
import com.gativah.admin.moderation.model.Appeal;
import com.gativah.admin.moderation.model.ContentReport;
import com.gativah.admin.moderation.model.ModerationAction;
import com.gativah.admin.moderation.query.ModerationQuery;
import com.gativah.admin.moderation.repo.AppealRepository;
import com.gativah.admin.moderation.repo.ContentReportRepository;
import com.gativah.admin.moderation.repo.ModerationActionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ModerationServiceImplTest {

    @Mock ModerationQuery query;
    @Mock ContentReportRepository reports;
    @Mock ModerationActionRepository actions;
    @Mock AppealRepository appeals;
    @Mock PacegritInternalClient internal;
    @Mock AuditService audit;

    ModerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModerationServiceImpl(query, reports, actions, appeals, internal, audit);
    }

    private ContentReport report() {
        ContentReport r = new ContentReport();
        r.setId(77L);
        r.setContentType("POST");
        r.setContentId(9L);
        r.setStatus(ContentReport.STATUS_PENDING);
        return r;
    }

    @Test
    void dismiss_marks_dismissed_without_side_effects() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));

        ResolveResponse res = service.resolve(5L, 77L, new ResolveRequest(ResolveAction.DISMISS, "ok", null));

        assertThat(res.status()).isEqualTo(ContentReport.STATUS_DISMISSED);
        verify(internal, never()).takedown(any(), any(), any(), any());
        verify(actions).save(any(ModerationAction.class));
        verify(audit).record(eq(5L), eq("REPORT_RESOLVE"), eq("REPORT"), eq("77"), any(), any(), any());
    }

    @Test
    void takedown_calls_internal_hook_and_resolves() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));

        ResolveResponse res = service.resolve(5L, 77L, new ResolveRequest(ResolveAction.TAKEDOWN, "spam", null));

        assertThat(res.status()).isEqualTo(ContentReport.STATUS_RESOLVED);
        verify(internal).takedown(5L, "POST", 9L, "spam");
    }

    @Test
    void warn_records_only() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));

        service.resolve(5L, 77L, new ResolveRequest(ResolveAction.WARN, "be nice", null));

        verify(internal, never()).takedown(any(), any(), any(), any());
        verify(internal, never()).banUser(any(), any(), any());
        verify(actions).save(any(ModerationAction.class));
    }

    @Test
    void suspend_resolves_author_and_calls_hook() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));
        when(query.authorOf("POST", 9L)).thenReturn(50L);

        service.resolve(5L, 77L, new ResolveRequest(ResolveAction.SUSPEND, "tos", 3));

        verify(internal).suspendUser(eq(5L), eq(50L), eq("tos"), any(LocalDateTime.class));
        ArgumentCaptor<ModerationAction> cap = ArgumentCaptor.forClass(ModerationAction.class);
        verify(actions).save(cap.capture());
        assertThat(cap.getValue().getTargetType()).isEqualTo("USER");
        assertThat(cap.getValue().getTargetId()).isEqualTo(50L);
    }

    @Test
    void ban_resolves_author_and_calls_hook() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));
        when(query.authorOf("POST", 9L)).thenReturn(50L);

        service.resolve(5L, 77L, new ResolveRequest(ResolveAction.BAN, "fraud", null));

        verify(internal).banUser(5L, 50L, "fraud");
    }

    @Test
    void resolve_missing_report_is_404() {
        when(reports.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(5L, 404L, new ResolveRequest(ResolveAction.DISMISS, null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void ban_without_resolvable_author_is_422() {
        when(reports.findById(77L)).thenReturn(Optional.of(report()));
        when(query.authorOf("POST", 9L)).thenReturn(null);

        assertThatThrownBy(() -> service.resolve(5L, 77L, new ResolveRequest(ResolveAction.BAN, "x", null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(internal, never()).banUser(any(), any(), any());
    }

    @Test
    void granting_an_appeal_reinstates_the_user() {
        Appeal a = new Appeal();
        a.setId(3L);
        a.setSubjectUserId(50L);
        a.setStatus(Appeal.STATUS_OPEN);
        when(appeals.findById(3L)).thenReturn(Optional.of(a));

        service.resolveAppeal(5L, 3L, new AppealResolveRequest(true, "valid"));

        assertThat(a.getStatus()).isEqualTo(Appeal.STATUS_GRANTED);
        verify(internal).reinstateUser(5L, 50L);
    }

    @Test
    void denying_an_appeal_does_not_reinstate() {
        Appeal a = new Appeal();
        a.setId(3L);
        a.setSubjectUserId(50L);
        a.setStatus(Appeal.STATUS_OPEN);
        when(appeals.findById(3L)).thenReturn(Optional.of(a));

        service.resolveAppeal(5L, 3L, new AppealResolveRequest(false, "no"));

        assertThat(a.getStatus()).isEqualTo(Appeal.STATUS_DENIED);
        verify(internal, never()).reinstateUser(any(), any());
    }
}

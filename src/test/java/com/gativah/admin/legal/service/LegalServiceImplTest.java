package com.gativah.admin.legal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.legal.dto.ApprovalRequest;
import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.UpdateLegalRequest;
import com.gativah.admin.legal.model.LegalDisclosure;
import com.gativah.admin.legal.model.LegalRequest;
import com.gativah.admin.legal.repo.LegalCorrespondenceRepository;
import com.gativah.admin.legal.repo.LegalCustodyEventRepository;
import com.gativah.admin.legal.repo.LegalDisclosureRepository;
import com.gativah.admin.legal.repo.LegalRequestRepository;
import com.gativah.admin.legal.repo.LegalTaskRepository;
import com.gativah.admin.legal.query.LegalQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LegalServiceImplTest {

    @Mock LegalRequestRepository requests;
    @Mock LegalDisclosureRepository disclosures;
    @Mock LegalTaskRepository tasks;
    @Mock LegalCorrespondenceRepository correspondence;
    @Mock LegalCustodyEventRepository custody;
    @Mock LegalQuery query;
    @Mock AuditService audit;

    LegalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LegalServiceImpl(requests, disclosures, tasks, correspondence, custody, query, audit);
    }

    private LegalRequest request(Long id, String status) {
        LegalRequest r = new LegalRequest();
        r.setId(id);
        r.setReference("CASE-1");
        r.setRequestType("SUBPOENA");
        r.setRequestingAuthority("District Court");
        r.setStatus(status);
        return r;
    }

    @Test
    void create_persists_received_request_and_audits() {
        when(requests.existsByReferenceIgnoreCase("CASE-9")).thenReturn(false);
        when(requests.save(any(LegalRequest.class))).thenAnswer(inv -> {
            LegalRequest r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        var summary = service.create(1L,
                new CreateLegalRequest("CASE-9", "COURT_ORDER", "High Court", 42L, "all account data", null, null));

        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.status()).isEqualTo(LegalRequest.STATUS_RECEIVED);
        verify(audit).record(any(), eq("LEGAL_REQUEST_CREATE"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void create_rejects_duplicate_reference() {
        when(requests.existsByReferenceIgnoreCase("CASE-1")).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L,
                new CreateLegalRequest("CASE-1", "SUBPOENA", "Court", null, null, null, null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(requests, never()).save(any());
    }

    @Test
    void record_disclosure_actions_an_approved_request_and_audits() {
        LegalRequest r = request(7L, LegalRequest.STATUS_RECEIVED);
        r.setApprovalStatus(LegalRequest.APPROVAL_APPROVED);
        when(requests.findById(7L)).thenReturn(Optional.of(r));
        when(disclosures.save(any(LegalDisclosure.class))).thenAnswer(inv -> {
            LegalDisclosure d = inv.getArgument(0);
            d.setId(3L);
            return d;
        });

        DisclosureRow row = service.recordDisclosure(2L, 7L,
                new RecordDisclosureRequest("FBI", "account,content", "valid subpoena CASE-1"));

        assertThat(row.id()).isEqualTo(3L);
        assertThat(r.getStatus()).isEqualTo(LegalRequest.STATUS_ACTIONED);
        verify(audit).record(any(), eq("LEGAL_DISCLOSURE"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void record_disclosure_blocked_until_approved() {
        when(requests.findById(9L)).thenReturn(Optional.of(request(9L, LegalRequest.STATUS_RECEIVED)));
        assertThatThrownBy(() -> service.recordDisclosure(2L, 9L,
                new RecordDisclosureRequest("Agency", "account", "n/a")))
                .isInstanceOf(ResponseStatusException.class);
        verify(disclosures, never()).save(any());
    }

    @Test
    void approve_sets_status_and_logs_custody() {
        LegalRequest r = request(11L, LegalRequest.STATUS_RECEIVED);
        when(requests.findById(11L)).thenReturn(Optional.of(r));

        service.approve(1L, 11L, new ApprovalRequest(true, "sufficient"));

        assertThat(r.getApprovalStatus()).isEqualTo(LegalRequest.APPROVAL_APPROVED);
        assertThat(r.getApprovedBy()).isEqualTo(1L);
        verify(audit).record(eq(1L), eq("LEGAL_APPROVAL"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void record_disclosure_blocked_on_rejected_request() {
        when(requests.findById(8L)).thenReturn(Optional.of(request(8L, LegalRequest.STATUS_REJECTED)));
        assertThatThrownBy(() -> service.recordDisclosure(2L, 8L,
                new RecordDisclosureRequest("Agency", "account", "n/a")))
                .isInstanceOf(ResponseStatusException.class);
        verify(disclosures, never()).save(any());
    }

    @Test
    void update_rejects_invalid_status() {
        when(requests.findById(9L)).thenReturn(Optional.of(request(9L, LegalRequest.STATUS_RECEIVED)));
        assertThatThrownBy(() -> service.update(1L, 9L, new UpdateLegalRequest("BOGUS", null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(requests, never()).save(any());
    }

    @Test
    void detail_missing_is_404() {
        when(requests.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(404L)).isInstanceOf(ResponseStatusException.class);
    }
}

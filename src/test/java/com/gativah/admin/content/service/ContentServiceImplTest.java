package com.gativah.admin.content.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.content.query.ContentQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock ContentQuery query;
    @Mock PacegritInternalClient internal;
    @Mock AuditService audit;

    ContentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContentServiceImpl(query, internal, audit);
    }

    @Test
    void list_normalizes_type_status_and_wraps_query() {
        service.list(List.of("post"), "hi", List.of("active"), Pageable.ofSize(20));
        verify(query).search(eq(List.of("POST")), eq("%hi%"), eq(List.of(false)), any(Pageable.class));
    }

    @Test
    void list_passes_nulls_for_blank_or_unknown() {
        service.list(List.of("bogus"), "  ", List.of("weird"), Pageable.ofSize(20));
        verify(query).search(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void takedown_calls_internal_hook_and_audits() {
        service.takedown(5L, "comment", 9L, "spam");
        verify(internal).takedown(5L, "COMMENT", 9L, "spam");
        verify(audit).record(eq(5L), eq("CONTENT_TAKEDOWN"), eq("COMMENT"), eq("9"), anyString(), any(), any());
    }

    @Test
    void takedown_rejects_bad_type() {
        assertThatThrownBy(() -> service.takedown(5L, "club", 9L, "x"))
                .isInstanceOf(ResponseStatusException.class);
        verify(internal, never()).takedown(any(), any(), any(), any());
    }
}

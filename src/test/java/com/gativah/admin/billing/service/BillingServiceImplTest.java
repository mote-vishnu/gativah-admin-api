package com.gativah.admin.billing.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.billing.dto.GrantCompRequest;
import com.gativah.admin.billing.query.BillingQuery;
import com.gativah.admin.client.PacegritInternalClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock BillingQuery query;
    @Mock PacegritInternalClient internal;
    @Mock AuditService audit;

    BillingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillingServiceImpl(query, internal, audit);
    }

    @Test
    void entitlements_wraps_query_into_a_like_pattern() {
        service.entitlements("raj", "COMP", Pageable.ofSize(20));
        verify(query).entitlements(eq("%raj%"), eq("COMP"), any(Pageable.class));
    }

    @Test
    void entitlements_passes_null_for_blank_query() {
        service.entitlements("  ", null, Pageable.ofSize(20));
        verify(query).entitlements(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void grant_comp_calls_internal_hook_and_audits() {
        LocalDateTime exp = LocalDateTime.now().plusDays(30);
        service.grantComp(1L, new GrantCompRequest(42L, "plus", exp, "VIP"));
        verify(internal).grantComp(1L, 42L, "plus", exp, "VIP");
        verify(audit).record(eq(1L), eq("ENTITLEMENT_GRANT"), eq("USER"), eq("42"), anyString(), any(), any());
    }

    @Test
    void revoke_comp_calls_internal_hook_and_audits() {
        service.revokeComp(1L, 42L, "plus");
        verify(internal).revokeComp(1L, 42L, "plus");
        verify(audit).record(eq(1L), eq("ENTITLEMENT_REVOKE"), eq("USER"), eq("42"), anyString(), any(), any());
    }
}

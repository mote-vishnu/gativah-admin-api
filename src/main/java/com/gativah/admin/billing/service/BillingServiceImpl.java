package com.gativah.admin.billing.service;

import java.util.List;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.billing.dto.EntitlementDefRow;
import com.gativah.admin.billing.dto.EntitlementRow;
import com.gativah.admin.billing.dto.GrantCompRequest;
import com.gativah.admin.billing.dto.RefundRow;
import com.gativah.admin.billing.query.BillingQuery;
import com.gativah.admin.client.PacegritInternalClient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingServiceImpl implements BillingService {

    private final BillingQuery query;
    private final PacegritInternalClient internal;
    private final AuditService audit;

    public BillingServiceImpl(BillingQuery query, PacegritInternalClient internal, AuditService audit) {
        this.query = query;
        this.internal = internal;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementRow> entitlements(String q, String source, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim() + "%";
        return query.entitlements(like, source, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundRow> refunds(Pageable pageable) {
        return query.refunds(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntitlementDefRow> entitlementDefs() {
        return query.entitlementDefs();
    }

    @Override
    public void grantComp(Long actorAdminId, GrantCompRequest req) {
        internal.grantComp(actorAdminId, req.userId(), req.code(), req.expiresAt(), req.reason());
        audit.record(actorAdminId, "ENTITLEMENT_GRANT", "USER", String.valueOf(req.userId()),
                "comp " + req.code() + (req.reason() == null ? "" : " — " + req.reason()), null, null);
    }

    @Override
    public void revokeComp(Long actorAdminId, Long userId, String code) {
        internal.revokeComp(actorAdminId, userId, code);
        audit.record(actorAdminId, "ENTITLEMENT_REVOKE", "USER", String.valueOf(userId), "comp " + code, null, null);
    }
}

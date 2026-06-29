package com.gativah.admin.billing.service;

import java.util.List;

import com.gativah.admin.billing.dto.EntitlementDefRow;
import com.gativah.admin.billing.dto.EntitlementRow;
import com.gativah.admin.billing.dto.GrantCompRequest;
import com.gativah.admin.billing.dto.RefundRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillingService {

    Page<EntitlementRow> entitlements(String q, String source, Pageable pageable);

    Page<RefundRow> refunds(Pageable pageable);

    List<EntitlementDefRow> entitlementDefs();

    void grantComp(Long actorAdminId, GrantCompRequest req);

    void revokeComp(Long actorAdminId, Long userId, String code);
}

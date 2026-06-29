package com.gativah.admin.billing.query;

import java.util.List;

import com.gativah.admin.billing.dto.EntitlementDefRow;
import com.gativah.admin.billing.dto.EntitlementRow;
import com.gativah.admin.billing.dto.RefundRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillingQuery {

    Page<EntitlementRow> entitlements(String q, String source, Pageable pageable);

    Page<RefundRow> refunds(Pageable pageable);

    List<EntitlementDefRow> entitlementDefs();
}

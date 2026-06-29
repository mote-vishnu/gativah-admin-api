package com.gativah.admin.billing.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.billing.dto.EntitlementDefsResponse;
import com.gativah.admin.billing.dto.EntitlementRow;
import com.gativah.admin.billing.dto.GrantCompRequest;
import com.gativah.admin.billing.dto.RefundRow;
import com.gativah.admin.billing.dto.RevokeCompRequest;
import com.gativah.admin.billing.service.BillingService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Billing Ops — entitlement registry + refunds + comp grants. BILLING:VIEW (read) / BILLING:EDIT (grant/revoke). */
@RestController
public class AdminBillingController {

    private final BillingService service;

    public AdminBillingController(BillingService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/billing/entitlements")
    @PreAuthorize("hasAuthority('BILLING:VIEW')")
    public Page<EntitlementRow> entitlements(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String source,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return service.entitlements(q, source, pageable);
    }

    @GetMapping("/api/v1/admin/billing/refunds")
    @PreAuthorize("hasAuthority('BILLING:VIEW')")
    public Page<RefundRow> refunds(@PageableDefault(size = 20) Pageable pageable) {
        return service.refunds(pageable);
    }

    @GetMapping("/api/v1/admin/billing/entitlement-defs")
    @PreAuthorize("hasAuthority('BILLING:VIEW')")
    public EntitlementDefsResponse entitlementDefs() {
        return new EntitlementDefsResponse(service.entitlementDefs());
    }

    @PostMapping("/api/v1/admin/billing/entitlements/comp")
    @PreAuthorize("hasAuthority('BILLING:EDIT')")
    public ResponseEntity<Void> grantComp(@AuthenticationPrincipal AdminPrincipal principal,
                                          @Valid @RequestBody GrantCompRequest req) {
        service.grantComp(principal.id(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/billing/entitlements/revoke")
    @PreAuthorize("hasAuthority('BILLING:EDIT')")
    public ResponseEntity<Void> revokeComp(@AuthenticationPrincipal AdminPrincipal principal,
                                           @Valid @RequestBody RevokeCompRequest req) {
        service.revokeComp(principal.id(), req.userId(), req.code());
        return ResponseEntity.noContent().build();
    }
}

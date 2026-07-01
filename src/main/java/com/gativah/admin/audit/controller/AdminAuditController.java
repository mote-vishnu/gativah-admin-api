package com.gativah.admin.audit.controller;

import java.time.LocalDate;

import com.gativah.admin.audit.dto.AuditEntryRow;
import com.gativah.admin.audit.dto.AuditStats;
import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.security.AdminPrincipal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Operator audit feed. AUDIT:VIEW sees everyone; others see only their own. */
@RestController
public class AdminAuditController {

    private final AuditService audit;

    public AdminAuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping("/api/v1/admin/audit")
    @PreAuthorize("isAuthenticated()")
    public Page<AuditEntryRow> audit(@AuthenticationPrincipal AdminPrincipal principal,
                                     Authentication auth,
                                     @RequestParam(required = false) Long actorId,
                                     @RequestParam(required = false) String action,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     @PageableDefault(size = 25) Pageable pageable) {
        return audit.list(scope(principal, auth, actorId), action, category, null, null,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                q, pageable);
    }

    @GetMapping("/api/v1/admin/audit/stats")
    @PreAuthorize("isAuthenticated()")
    public AuditStats stats(@AuthenticationPrincipal AdminPrincipal principal, Authentication auth,
                            @RequestParam(required = false) Long actorId) {
        return audit.stats(scope(principal, auth, actorId));
    }

    /** AUDIT:VIEW holders may scope to any operator; everyone else is forced to their own entries. */
    private static Long scope(AdminPrincipal principal, Authentication auth, Long actorId) {
        boolean viewAll = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("AUDIT:VIEW"::equals);
        return viewAll ? actorId : principal.id();
    }
}

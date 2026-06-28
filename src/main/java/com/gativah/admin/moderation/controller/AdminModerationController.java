package com.gativah.admin.moderation.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.moderation.dto.AppealResolveRequest;
import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.ModerationActionRow;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportSummary;
import com.gativah.admin.moderation.dto.ResolveRequest;
import com.gativah.admin.moderation.dto.ResolveResponse;
import com.gativah.admin.moderation.service.ModerationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Grievance queue, report triage/resolution, moderation history, and appeals. */
@RestController
public class AdminModerationController {

    private final ModerationService service;

    public AdminModerationController(ModerationService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/reports")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public Page<ReportSummary> reports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String reason,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.queue(status, contentType, reason, pageable);
    }

    @GetMapping("/api/v1/admin/reports/{id}")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public ReportDetail report(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/api/v1/admin/reports/{id}/resolve")
    @PreAuthorize("hasAuthority('GRIEVANCES:EDIT')")
    public ResolveResponse resolve(@AuthenticationPrincipal AdminPrincipal principal,
                                   @PathVariable Long id, @Valid @RequestBody ResolveRequest req) {
        return service.resolve(principal.id(), id, req);
    }

    @GetMapping("/api/v1/admin/moderation/history")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public Page<ModerationActionRow> history(@PageableDefault(size = 20) Pageable pageable) {
        return service.history(pageable);
    }

    @GetMapping("/api/v1/admin/appeals")
    @PreAuthorize("hasAuthority('APPEALS:VIEW')")
    public Page<AppealRow> appeals(@RequestParam(required = false) String status,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return service.appeals(status, pageable);
    }

    @PostMapping("/api/v1/admin/appeals/{id}/resolve")
    @PreAuthorize("hasAuthority('APPEALS:EDIT')")
    public ResponseEntity<Void> resolveAppeal(@AuthenticationPrincipal AdminPrincipal principal,
                                              @PathVariable Long id, @RequestBody AppealResolveRequest req) {
        service.resolveAppeal(principal.id(), id, req);
        return ResponseEntity.noContent().build();
    }
}

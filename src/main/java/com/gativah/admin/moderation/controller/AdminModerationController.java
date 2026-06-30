package com.gativah.admin.moderation.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.moderation.dto.AppealResolveRequest;
import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.AssignRequest;
import com.gativah.admin.moderation.dto.AuthorHistory;
import com.gativah.admin.moderation.dto.BulkAssignRequest;
import com.gativah.admin.moderation.dto.BulkResolveRequest;
import com.gativah.admin.moderation.dto.BulkResolveResponse;
import com.gativah.admin.moderation.dto.ModerationActionRow;
import com.gativah.admin.moderation.dto.ReasonBreakdownResponse;
import com.gativah.admin.moderation.dto.RegionBansResponse;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportStats;
import com.gativah.admin.moderation.dto.SignalsResponse;
import com.gativah.admin.moderation.dto.TimelineResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
            @RequestParam(required = false) List<String> status,
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

    @PostMapping("/api/v1/admin/reports/bulk-resolve")
    @PreAuthorize("hasAuthority('GRIEVANCES:EDIT')")
    public BulkResolveResponse bulkResolve(@AuthenticationPrincipal AdminPrincipal principal,
                                           @Valid @RequestBody BulkResolveRequest req) {
        List<Long> failed = new ArrayList<>();
        int resolved = 0;
        for (Long id : req.ids()) {
            try {
                // Each call is its own transaction (proxied), so one failure can't abort the rest.
                service.resolve(principal.id(), id, new ResolveRequest(req.action(), req.reason(), null, null));
                resolved++;
            } catch (RuntimeException e) {
                failed.add(id);
            }
        }
        return new BulkResolveResponse(resolved, failed.size(), failed);
    }

    @PatchMapping("/api/v1/admin/reports/{id}/assign")
    @PreAuthorize("hasAuthority('GRIEVANCES:EDIT')")
    public ResponseEntity<Void> assign(@AuthenticationPrincipal AdminPrincipal principal,
                                       @PathVariable Long id, @RequestBody AssignRequest req) {
        service.assign(principal.id(), id, req.adminId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/admin/reports/bulk-assign")
    @PreAuthorize("hasAuthority('GRIEVANCES:EDIT')")
    public ResponseEntity<Void> bulkAssign(@AuthenticationPrincipal AdminPrincipal principal,
                                           @Valid @RequestBody BulkAssignRequest req) {
        service.bulkAssign(principal.id(), req.ids(), req.adminId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/reports/by-reason")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public ReasonBreakdownResponse byReason() {
        return new ReasonBreakdownResponse(service.queueByReason());
    }

    @GetMapping("/api/v1/admin/reports/stats")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public ReportStats stats() {
        return service.stats();
    }

    @GetMapping("/api/v1/admin/region-bans")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public RegionBansResponse regionBans() {
        return new RegionBansResponse(service.regionBans());
    }

    @PostMapping("/api/v1/admin/region-bans/{id}/lift")
    @PreAuthorize("hasAuthority('GRIEVANCES:EDIT')")
    public ResponseEntity<Void> liftRegionBan(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable Long id) {
        service.liftRegionBan(principal.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/reports/{id}/timeline")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public TimelineResponse timeline(@PathVariable Long id) {
        return new TimelineResponse(service.timeline(id));
    }

    @GetMapping("/api/v1/admin/reports/{id}/author-history")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public AuthorHistory authorHistory(@PathVariable Long id) {
        return service.authorHistory(id);
    }

    @GetMapping("/api/v1/admin/reports/{id}/signals")
    @PreAuthorize("hasAuthority('GRIEVANCES:VIEW')")
    public SignalsResponse signals(@PathVariable Long id) {
        return new SignalsResponse(service.signals(id));
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

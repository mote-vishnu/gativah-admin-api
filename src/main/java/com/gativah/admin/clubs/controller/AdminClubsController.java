package com.gativah.admin.clubs.controller;

import java.util.List;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.clubs.dto.ClubActionRequest;
import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubSummary;
import com.gativah.admin.clubs.service.ClubAdminService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Clubs directory + moderation actions. Read gated by CLUBS:VIEW, writes by CLUBS:EDIT. */
@RestController
public class AdminClubsController {

    private final ClubAdminService service;

    public AdminClubsController(ClubAdminService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/clubs")
    @PreAuthorize("hasAuthority('CLUBS:VIEW')")
    public Page<ClubSummary> clubs(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) List<String> visibility,
                                   @RequestParam(required = false) List<String> status,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, visibility, status, pageable);
    }

    @GetMapping("/api/v1/admin/clubs/{id}")
    @PreAuthorize("hasAuthority('CLUBS:VIEW')")
    public ClubDetail club(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/api/v1/admin/clubs/{id}/remove")
    @PreAuthorize("hasAuthority('CLUBS:EDIT')")
    public ClubDetail remove(@AuthenticationPrincipal AdminPrincipal principal,
                             @PathVariable Long id, @RequestBody(required = false) ClubActionRequest req) {
        return service.removeClub(principal.id(), id, req == null ? null : req.reason());
    }

    @PostMapping("/api/v1/admin/clubs/{id}/restore")
    @PreAuthorize("hasAuthority('CLUBS:EDIT')")
    public ClubDetail restore(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable Long id) {
        return service.restoreClub(principal.id(), id);
    }

    @PostMapping("/api/v1/admin/clubs/{id}/members/{userId}/remove")
    @PreAuthorize("hasAuthority('CLUBS:EDIT')")
    public ClubDetail removeMember(@AuthenticationPrincipal AdminPrincipal principal,
                                   @PathVariable Long id, @PathVariable Long userId) {
        return service.removeMember(principal.id(), id, userId);
    }

    @PostMapping("/api/v1/admin/clubs/{id}/events/{eventId}/remove")
    @PreAuthorize("hasAuthority('CLUBS:EDIT')")
    public ClubDetail removeEvent(@AuthenticationPrincipal AdminPrincipal principal,
                                  @PathVariable Long id, @PathVariable Long eventId,
                                  @RequestBody(required = false) ClubActionRequest req) {
        return service.removeEvent(principal.id(), id, eventId, req == null ? null : req.reason());
    }
}

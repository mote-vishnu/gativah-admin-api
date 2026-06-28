package com.gativah.admin.clubs.controller;

import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubSummary;
import com.gativah.admin.clubs.service.ClubAdminService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Clubs directory (read-only). Gated by CLUBS:VIEW. */
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
}

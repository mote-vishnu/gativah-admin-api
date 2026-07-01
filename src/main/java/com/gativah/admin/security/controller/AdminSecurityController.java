package com.gativah.admin.security.controller;

import com.gativah.admin.security.dto.ActiveSessionsResponse;
import com.gativah.admin.security.dto.SecurityOverview;
import com.gativah.admin.security.service.SecurityService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Org-wide admin security posture (MFA coverage, active sessions). Read gated by
 * STAFF:VIEW; session revocation reuses the staff endpoint (STAFF:EDIT).
 */
@RestController
public class AdminSecurityController {

    private final SecurityService service;

    public AdminSecurityController(SecurityService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/security/overview")
    @PreAuthorize("hasAuthority('STAFF:VIEW')")
    public SecurityOverview overview() {
        return service.overview();
    }

    @GetMapping("/api/v1/admin/security/sessions")
    @PreAuthorize("hasAuthority('STAFF:VIEW')")
    public ActiveSessionsResponse sessions() {
        return new ActiveSessionsResponse(service.activeSessions());
    }
}

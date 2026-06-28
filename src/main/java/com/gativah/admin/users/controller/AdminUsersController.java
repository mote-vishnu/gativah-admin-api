package com.gativah.admin.users.controller;

import java.util.List;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.users.dto.BanRequest;
import com.gativah.admin.users.dto.SuspendRequest;
import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.dto.UserSummary;
import com.gativah.admin.users.service.UserAdminService;

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

/** Users 360 — directory, profile, and account actions. USERS:VIEW (read) / USERS:EDIT (actions). */
@RestController
public class AdminUsersController {

    private final UserAdminService service;

    public AdminUsersController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/users")
    @PreAuthorize("hasAuthority('USERS:VIEW')")
    public Page<UserSummary> users(@RequestParam(required = false) String q,
                                   @RequestParam(required = false) List<String> status,
                                   @PageableDefault(size = 20) Pageable pageable) {
        return service.list(q, status, pageable);
    }

    @GetMapping("/api/v1/admin/users/{id}")
    @PreAuthorize("hasAuthority('USERS:VIEW')")
    public UserDetail user(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/api/v1/admin/users/{id}/suspend")
    @PreAuthorize("hasAuthority('USERS:EDIT')")
    public UserDetail suspend(@AuthenticationPrincipal AdminPrincipal principal,
                              @PathVariable Long id, @RequestBody SuspendRequest req) {
        return service.suspend(principal.id(), id, req);
    }

    @PostMapping("/api/v1/admin/users/{id}/ban")
    @PreAuthorize("hasAuthority('USERS:EDIT')")
    public UserDetail ban(@AuthenticationPrincipal AdminPrincipal principal,
                          @PathVariable Long id, @RequestBody BanRequest req) {
        return service.ban(principal.id(), id, req);
    }

    @PostMapping("/api/v1/admin/users/{id}/reinstate")
    @PreAuthorize("hasAuthority('USERS:EDIT')")
    public UserDetail reinstate(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable Long id) {
        return service.reinstate(principal.id(), id);
    }
}

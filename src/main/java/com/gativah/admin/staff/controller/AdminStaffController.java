package com.gativah.admin.staff.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.staff.dto.AssignRolesRequest;
import com.gativah.admin.staff.dto.InviteStaffRequest;
import com.gativah.admin.staff.dto.StaffRow;
import com.gativah.admin.staff.dto.UpdateStaffRequest;
import com.gativah.admin.staff.service.StaffService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Staff directory + invite/role assignment. Gated per-action by STAFF:VIEW/ADD/EDIT. */
@RestController
public class AdminStaffController {

    private final StaffService service;

    public AdminStaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/staff")
    @PreAuthorize("hasAuthority('STAFF:VIEW')")
    public Page<StaffRow> staff(@PageableDefault(size = 25) Pageable pageable) {
        return service.list(pageable);
    }

    @PostMapping("/api/v1/admin/staff")
    @PreAuthorize("hasAuthority('STAFF:ADD')")
    public StaffRow invite(@AuthenticationPrincipal AdminPrincipal principal,
                           @Valid @RequestBody InviteStaffRequest req) {
        return service.invite(principal.id(), req);
    }

    @PatchMapping("/api/v1/admin/staff/{id}")
    @PreAuthorize("hasAuthority('STAFF:EDIT')")
    public StaffRow update(@AuthenticationPrincipal AdminPrincipal principal,
                           @PathVariable Long id, @RequestBody UpdateStaffRequest req) {
        return service.update(principal.id(), id, req);
    }

    @PatchMapping("/api/v1/admin/staff/{id}/roles")
    @PreAuthorize("hasAuthority('STAFF:EDIT')")
    public StaffRow setRoles(@AuthenticationPrincipal AdminPrincipal principal,
                             @PathVariable Long id, @Valid @RequestBody AssignRolesRequest req) {
        return service.setRoles(principal.id(), id, req.roleIds());
    }
}

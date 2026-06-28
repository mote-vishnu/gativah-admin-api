package com.gativah.admin.roles.controller;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.roles.dto.CreateRoleRequest;
import com.gativah.admin.roles.dto.PermissionCatalogResponse;
import com.gativah.admin.roles.dto.RoleResponse;
import com.gativah.admin.roles.dto.RolesResponse;
import com.gativah.admin.roles.dto.UpdateRoleRequest;
import com.gativah.admin.roles.service.RoleService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Roles & permissions administration. Gated per-action by ROLES:VIEW/ADD/EDIT/DELETE. */
@RestController
public class AdminRolesController {

    private final RoleService service;

    public AdminRolesController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/roles")
    @PreAuthorize("hasAuthority('ROLES:VIEW')")
    public RolesResponse roles() {
        return service.listRoles();
    }

    @GetMapping("/api/v1/admin/permissions")
    @PreAuthorize("hasAuthority('ROLES:VIEW')")
    public PermissionCatalogResponse permissions() {
        return service.catalog();
    }

    @PostMapping("/api/v1/admin/roles")
    @PreAuthorize("hasAuthority('ROLES:ADD')")
    public RoleResponse create(@AuthenticationPrincipal AdminPrincipal principal,
                               @Valid @RequestBody CreateRoleRequest req) {
        return service.create(principal.id(), req);
    }

    @PatchMapping("/api/v1/admin/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES:EDIT')")
    public RoleResponse update(@AuthenticationPrincipal AdminPrincipal principal,
                               @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest req) {
        return service.update(principal.id(), id, req);
    }

    @DeleteMapping("/api/v1/admin/roles/{id}")
    @PreAuthorize("hasAuthority('ROLES:DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AdminPrincipal principal, @PathVariable Long id) {
        service.delete(principal.id(), id);
    }
}

package com.gativah.admin.roles.service;

import com.gativah.admin.roles.dto.CreateRoleRequest;
import com.gativah.admin.roles.dto.PermissionCatalogResponse;
import com.gativah.admin.roles.dto.RoleResponse;
import com.gativah.admin.roles.dto.RolesResponse;
import com.gativah.admin.roles.dto.UpdateRoleRequest;

public interface RoleService {

    RolesResponse listRoles();

    PermissionCatalogResponse catalog();

    RoleResponse create(Long actorAdminId, CreateRoleRequest req);

    RoleResponse update(Long actorAdminId, Long id, UpdateRoleRequest req);

    void delete(Long actorAdminId, Long id);
}

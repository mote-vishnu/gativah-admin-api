package com.gativah.admin.roles.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.model.AdminFeature;
import com.gativah.admin.auth.model.AdminPermission;
import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.repo.AdminFeatureRepository;
import com.gativah.admin.auth.repo.AdminPermissionRepository;
import com.gativah.admin.auth.repo.AdminRoleRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.roles.dto.CreateRoleRequest;
import com.gativah.admin.roles.dto.FeaturePermissions;
import com.gativah.admin.roles.dto.PermissionCatalogResponse;
import com.gativah.admin.roles.dto.PermissionResponse;
import com.gativah.admin.roles.dto.RoleResponse;
import com.gativah.admin.roles.dto.RolesResponse;
import com.gativah.admin.roles.dto.UpdateRoleRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleServiceImpl implements RoleService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminRoleRepository roleRepo;
    private final AdminPermissionRepository permissionRepo;
    private final AdminFeatureRepository featureRepo;
    private final AdminUserRepository userRepo;
    private final AuditService audit;

    public RoleServiceImpl(AdminRoleRepository roleRepo, AdminPermissionRepository permissionRepo,
                           AdminFeatureRepository featureRepo, AdminUserRepository userRepo,
                           AuditService audit) {
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
        this.featureRepo = featureRepo;
        this.userRepo = userRepo;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public RolesResponse listRoles() {
        List<RoleResponse> rows = roleRepo.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
        return new RolesResponse(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionCatalogResponse catalog() {
        List<AdminPermission> permissions = permissionRepo.findAllByOrderByCodeAsc();
        List<FeaturePermissions> features = featureRepo.findAllByOrderBySortOrderAsc().stream()
                .map(f -> new FeaturePermissions(
                        f.getCode(),
                        f.getLabel(),
                        f.getSortOrder(),
                        permissions.stream()
                                .filter(p -> p.getFeature().getId().equals(f.getId()))
                                .map(RoleServiceImpl::toPermission)
                                .toList()))
                .toList();
        return new PermissionCatalogResponse(features);
    }

    @Override
    @Transactional
    public RoleResponse create(Long actorAdminId, CreateRoleRequest req) {
        String name = req.name().trim();
        if (roleRepo.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already in use: " + name);
        }
        AdminRole role = new AdminRole();
        role.setName(name);
        role.setDescription(trimToNull(req.description()));
        role.setSystem(false);
        role.setPermissions(resolvePermissions(req.permissionIds()));
        role = roleRepo.save(role);
        audit.record(actorAdminId, "ROLE_CREATE", "ROLE", String.valueOf(role.getId()),
                name + " (" + role.getPermissions().size() + " permissions)", null, null);
        return toResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse update(Long actorAdminId, Long id, UpdateRoleRequest req) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + id));
        // SUPER_ADMIN is the safety net — its permission set must stay complete, so it's immutable.
        if (SUPER_ADMIN.equals(role.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SUPER_ADMIN cannot be modified");
        }
        if (req.name() != null && !req.name().trim().isEmpty()) {
            // A built-in role's name is referenced in code/migrations — only custom roles can be renamed.
            if (role.isSystem()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A system role's name cannot be changed");
            }
            String name = req.name().trim();
            roleRepo.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Role name already in use: " + name);
                }
            });
            role.setName(name);
        }
        if (req.description() != null) {
            role.setDescription(trimToNull(req.description()));
        }
        if (req.permissionIds() != null) {
            role.setPermissions(resolvePermissions(req.permissionIds()));
        }
        role = roleRepo.save(role);
        audit.record(actorAdminId, "ROLE_UPDATE", "ROLE", String.valueOf(id),
                role.getName() + " (" + role.getPermissions().size() + " permissions)", null, null);
        return toResponse(role);
    }

    @Override
    @Transactional
    public void delete(Long actorAdminId, Long id) {
        AdminRole role = roleRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found: " + id));
        if (role.isSystem()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System roles cannot be deleted");
        }
        long assigned = userRepo.countByRoles_Id(id);
        if (assigned > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Role is assigned to " + assigned + " user(s); unassign before deleting");
        }
        roleRepo.delete(role);
        audit.record(actorAdminId, "ROLE_DELETE", "ROLE", String.valueOf(id), role.getName(), null, null);
    }

    private Set<AdminPermission> resolvePermissions(List<Long> permissionIds) {
        Set<AdminPermission> permissions = new LinkedHashSet<>();
        for (Long permissionId : permissionIds) {
            AdminPermission p = permissionRepo.findById(permissionId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Permission not found: " + permissionId));
            permissions.add(p);
        }
        return permissions;
    }

    private RoleResponse toResponse(AdminRole role) {
        List<Long> permissionIds = role.getPermissions().stream()
                .map(AdminPermission::getId).sorted().toList();
        List<String> codes = role.getPermissions().stream()
                .map(AdminPermission::getCode).sorted().toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isSystem(),
                permissionIds, codes, userRepo.countByRoles_Id(role.getId()));
    }

    private static PermissionResponse toPermission(AdminPermission p) {
        AdminFeature feature = p.getFeature();
        return new PermissionResponse(p.getId(), p.getCode(), p.getAction(), feature.getCode());
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

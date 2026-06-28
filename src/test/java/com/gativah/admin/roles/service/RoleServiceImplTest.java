package com.gativah.admin.roles.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import com.gativah.admin.roles.dto.PermissionCatalogResponse;
import com.gativah.admin.roles.dto.RoleResponse;
import com.gativah.admin.roles.dto.UpdateRoleRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock AdminRoleRepository roleRepo;
    @Mock AdminPermissionRepository permissionRepo;
    @Mock AdminFeatureRepository featureRepo;
    @Mock AdminUserRepository userRepo;
    @Mock AuditService audit;

    RoleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RoleServiceImpl(roleRepo, permissionRepo, featureRepo, userRepo, audit);
    }

    private static AdminFeature feature(Long id, String code, int sort) {
        AdminFeature f = new AdminFeature();
        f.setId(id);
        f.setCode(code);
        f.setLabel(code);
        f.setSortOrder(sort);
        return f;
    }

    private static AdminPermission permission(Long id, AdminFeature feature, String action) {
        AdminPermission p = new AdminPermission();
        p.setId(id);
        p.setFeature(feature);
        p.setAction(action);
        p.setCode(feature.getCode() + ":" + action);
        return p;
    }

    @Test
    void catalog_groups_permissions_by_feature() {
        AdminFeature finance = feature(1L, "FINANCE", 0);
        AdminFeature staff = feature(2L, "STAFF", 1);
        when(featureRepo.findAllByOrderBySortOrderAsc()).thenReturn(List.of(finance, staff));
        when(permissionRepo.findAllByOrderByCodeAsc()).thenReturn(List.of(
                permission(10L, finance, "VIEW"),
                permission(20L, staff, "VIEW"),
                permission(21L, staff, "ADD")));

        PermissionCatalogResponse catalog = service.catalog();

        assertThat(catalog.features()).hasSize(2);
        assertThat(catalog.features().get(0).featureCode()).isEqualTo("FINANCE");
        assertThat(catalog.features().get(0).permissions()).hasSize(1);
        assertThat(catalog.features().get(1).permissions()).hasSize(2);
    }

    @Test
    void create_resolves_permissions_and_audits() {
        AdminFeature finance = feature(1L, "FINANCE", 0);
        when(roleRepo.findByName("Auditor")).thenReturn(Optional.empty());
        when(permissionRepo.findById(10L)).thenReturn(Optional.of(permission(10L, finance, "VIEW")));
        when(roleRepo.save(any(AdminRole.class))).thenAnswer(inv -> {
            AdminRole r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });
        when(userRepo.countByRoles_Id(7L)).thenReturn(0L);

        RoleResponse res = service.create(1L, new CreateRoleRequest("Auditor", "Read finance", List.of(10L)));

        assertThat(res.id()).isEqualTo(7L);
        assertThat(res.system()).isFalse();
        assertThat(res.permissions()).containsExactly("FINANCE:VIEW");
        verify(audit).record(any(), eq("ROLE_CREATE"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void create_rejects_duplicate_name() {
        when(roleRepo.findByName("Dup")).thenReturn(Optional.of(new AdminRole()));
        assertThatThrownBy(() -> service.create(1L, new CreateRoleRequest("Dup", null, List.of())))
                .isInstanceOf(ResponseStatusException.class);
        verify(roleRepo, never()).save(any());
    }

    @Test
    void update_rejects_system_role() {
        AdminRole sys = new AdminRole();
        sys.setId(1L);
        sys.setName("SUPER_ADMIN");
        sys.setSystem(true);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(sys));

        assertThatThrownBy(() -> service.update(1L, 1L, new UpdateRoleRequest("x", null, null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(roleRepo, never()).save(any());
    }

    @Test
    void delete_rejects_system_role() {
        AdminRole sys = new AdminRole();
        sys.setId(1L);
        sys.setSystem(true);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(sys));

        assertThatThrownBy(() -> service.delete(1L, 1L)).isInstanceOf(ResponseStatusException.class);
        verify(roleRepo, never()).delete(any());
    }

    @Test
    void delete_rejects_assigned_role() {
        AdminRole role = new AdminRole();
        role.setId(5L);
        role.setSystem(false);
        when(roleRepo.findById(5L)).thenReturn(Optional.of(role));
        when(userRepo.countByRoles_Id(5L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L, 5L)).isInstanceOf(ResponseStatusException.class);
        verify(roleRepo, never()).delete(any());
    }

    @Test
    void delete_removes_unassigned_custom_role() {
        AdminRole role = new AdminRole();
        role.setId(5L);
        role.setName("Temp");
        role.setSystem(false);
        role.setPermissions(Set.of());
        when(roleRepo.findById(5L)).thenReturn(Optional.of(role));
        when(userRepo.countByRoles_Id(5L)).thenReturn(0L);

        service.delete(1L, 5L);

        verify(roleRepo).delete(role);
        verify(audit).record(any(), eq("ROLE_DELETE"), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void update_missing_is_404() {
        when(roleRepo.findById(anyLong())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(1L, 404L, new UpdateRoleRequest("x", null, null)))
                .isInstanceOf(ResponseStatusException.class);
    }
}

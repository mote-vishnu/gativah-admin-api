package com.gativah.admin.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class AdminUserPermissionTest {

    private static AdminFeature feature(Long id, String code) {
        AdminFeature f = new AdminFeature();
        f.setId(id);
        f.setCode(code);
        f.setLabel(code);
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

    private static AdminRole role(String name, AdminPermission... permissions) {
        AdminRole r = new AdminRole();
        r.setName(name);
        r.setPermissions(Set.of(permissions));
        return r;
    }

    @Test
    void permission_codes_are_the_union_across_roles_distinct_and_sorted() {
        AdminFeature finance = feature(1L, "FINANCE");
        AdminFeature grievances = feature(2L, "GRIEVANCES");
        AdminPermission financeView = permission(10L, finance, "VIEW");
        AdminPermission grievancesView = permission(20L, grievances, "VIEW");
        AdminPermission grievancesEdit = permission(21L, grievances, "EDIT");

        AdminRole analyst = role("FINANCE_ANALYST", financeView, grievancesView);
        AdminRole moderator = role("MODERATOR", grievancesView, grievancesEdit);

        AdminUser u = new AdminUser();
        u.setRoles(Set.of(analyst, moderator));

        assertThat(u.permissionCodes())
                .containsExactly("FINANCE:VIEW", "GRIEVANCES:EDIT", "GRIEVANCES:VIEW");
    }

    @Test
    void role_names_are_sorted() {
        AdminUser u = new AdminUser();
        u.setRoles(Set.of(role("SUPPORT"), role("FINANCE_ANALYST")));
        assertThat(u.roleNames()).containsExactly("FINANCE_ANALYST", "SUPPORT");
    }

    @Test
    void no_roles_means_no_permissions() {
        AdminUser u = new AdminUser();
        assertThat(u.permissionCodes()).isEmpty();
        assertThat(u.roleNames()).isEmpty();
    }
}

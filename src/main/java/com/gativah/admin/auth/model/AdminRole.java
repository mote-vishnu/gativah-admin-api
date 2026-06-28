package com.gativah.admin.auth.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Staff roles and their capability matrix (Phase 1 = code-defined, table-driven
 * RBAC deferred to P2). Authorities exposed to Spring Security are
 * {@code ROLE_<name>} plus each {@link AdminPermission} name.
 */
public enum AdminRole {

    SUPER_ADMIN(EnumSet.allOf(AdminPermission.class)),

    MODERATOR(EnumSet.of(
            AdminPermission.MODERATION_REVIEW,
            AdminPermission.MODERATION_ACTION,
            AdminPermission.APPEAL_HANDLE)),

    FINANCE_ANALYST(EnumSet.of(
            AdminPermission.FINANCE_VIEW,
            AdminPermission.ENTITLEMENT_GRANT)),

    SUPPORT(EnumSet.of(
            AdminPermission.MODERATION_REVIEW,
            AdminPermission.FINANCE_VIEW));

    private final Set<AdminPermission> permissions;

    AdminRole(Set<AdminPermission> permissions) {
        this.permissions = permissions;
    }

    public Set<AdminPermission> permissions() {
        return permissions;
    }

    public boolean has(AdminPermission permission) {
        return permissions.contains(permission);
    }

    /** Spring authorities: the role (ROLE_*) plus every granted permission. */
    public List<String> authorities() {
        List<String> out = new ArrayList<>();
        out.add("ROLE_" + name());
        for (AdminPermission p : permissions) {
            out.add(p.name());
        }
        return out;
    }
}

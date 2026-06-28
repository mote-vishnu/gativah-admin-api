package com.gativah.admin.auth.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminRolePermissionTest {

    @Test
    void super_admin_has_every_permission() {
        for (AdminPermission p : AdminPermission.values()) {
            assertThat(AdminRole.SUPER_ADMIN.has(p)).as(p.name()).isTrue();
        }
        assertThat(AdminRole.SUPER_ADMIN.authorities()).contains("ROLE_SUPER_ADMIN", "LEGAL_ACCESS");
    }

    @Test
    void moderator_can_moderate_but_not_see_finance() {
        assertThat(AdminRole.MODERATOR.has(AdminPermission.MODERATION_ACTION)).isTrue();
        assertThat(AdminRole.MODERATOR.has(AdminPermission.FINANCE_VIEW)).isFalse();
        assertThat(AdminRole.MODERATOR.has(AdminPermission.STAFF_MANAGE)).isFalse();
        assertThat(AdminRole.MODERATOR.authorities())
                .contains("ROLE_MODERATOR", "MODERATION_ACTION")
                .doesNotContain("FINANCE_VIEW");
    }

    @Test
    void finance_analyst_sees_finance_not_moderation() {
        assertThat(AdminRole.FINANCE_ANALYST.has(AdminPermission.FINANCE_VIEW)).isTrue();
        assertThat(AdminRole.FINANCE_ANALYST.has(AdminPermission.ENTITLEMENT_GRANT)).isTrue();
        assertThat(AdminRole.FINANCE_ANALYST.has(AdminPermission.MODERATION_ACTION)).isFalse();
    }

    @Test
    void only_super_admin_manages_staff_and_legal() {
        assertThat(AdminRole.SUPPORT.has(AdminPermission.STAFF_MANAGE)).isFalse();
        assertThat(AdminRole.MODERATOR.has(AdminPermission.LEGAL_ACCESS)).isFalse();
        assertThat(AdminRole.FINANCE_ANALYST.has(AdminPermission.STAFF_MANAGE)).isFalse();
    }
}

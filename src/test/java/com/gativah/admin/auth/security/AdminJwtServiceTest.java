package com.gativah.admin.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import com.gativah.admin.auth.model.AdminFeature;
import com.gativah.admin.auth.model.AdminPermission;
import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;

import org.junit.jupiter.api.Test;

class AdminJwtServiceTest {

    // base64 of "0123456789abcdef0123456789abcdef" (32 bytes)
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private static AdminPermission permission(String featureCode, String action) {
        AdminFeature f = new AdminFeature();
        f.setCode(featureCode);
        f.setLabel(featureCode);
        AdminPermission p = new AdminPermission();
        p.setFeature(f);
        p.setAction(action);
        p.setCode(featureCode + ":" + action);
        return p;
    }

    private AdminUser user() {
        AdminRole moderator = new AdminRole();
        moderator.setName("MODERATOR");
        moderator.setPermissions(Set.of(
                permission("GRIEVANCES", "VIEW"),
                permission("GRIEVANCES", "EDIT")));
        AdminUser u = new AdminUser();
        u.setId(42L);
        u.setEmail("dev@gativah.com");
        u.setName("Dev K.");
        u.setRoles(Set.of(moderator));
        return u;
    }

    @Test
    void round_trips_principal_and_authorities() {
        AdminJwtService svc = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        AdminUser u = user();
        String token = svc.generate(u, u.permissionCodes());

        AdminJwtService.Parsed parsed = svc.parse(token);
        assertThat(parsed.principal().id()).isEqualTo(42L);
        assertThat(parsed.principal().email()).isEqualTo("dev@gativah.com");
        assertThat(parsed.principal().roles()).containsExactly("MODERATOR");
        assertThat(parsed.authorities()).contains("GRIEVANCES:VIEW", "GRIEVANCES:EDIT");
    }

    @Test
    void rejects_a_token_signed_with_another_secret() {
        AdminJwtService issuer = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        AdminJwtService verifier = new AdminJwtService(OTHER_SECRET, "gativah-admin", 1_800_000);
        String token = issuer.generate(user(), List.of("GRIEVANCES:VIEW"));

        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(Exception.class);
    }

    @Test
    void rejects_a_token_with_the_wrong_audience() {
        AdminJwtService issuer = new AdminJwtService(SECRET, "some-other-aud", 1_800_000);
        AdminJwtService verifier = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        String token = issuer.generate(user(), List.of("GRIEVANCES:VIEW"));

        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(IllegalArgumentException.class);
    }
}

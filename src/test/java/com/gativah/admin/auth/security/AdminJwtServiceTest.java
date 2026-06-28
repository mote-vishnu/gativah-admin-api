package com.gativah.admin.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;

import org.junit.jupiter.api.Test;

class AdminJwtServiceTest {

    // base64 of "0123456789abcdef0123456789abcdef" (32 bytes)
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private AdminUser user() {
        AdminUser u = new AdminUser();
        u.setId(42L);
        u.setEmail("dev@gativah.com");
        u.setName("Dev K.");
        u.setRole(AdminRole.MODERATOR);
        return u;
    }

    @Test
    void round_trips_principal_and_authorities() {
        AdminJwtService svc = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        String token = svc.generate(user(), AdminRole.MODERATOR.authorities());

        AdminJwtService.Parsed parsed = svc.parse(token);
        assertThat(parsed.principal().id()).isEqualTo(42L);
        assertThat(parsed.principal().email()).isEqualTo("dev@gativah.com");
        assertThat(parsed.principal().role()).isEqualTo(AdminRole.MODERATOR);
        assertThat(parsed.authorities()).contains("ROLE_MODERATOR", "MODERATION_ACTION");
    }

    @Test
    void rejects_a_token_signed_with_another_secret() {
        AdminJwtService issuer = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        AdminJwtService verifier = new AdminJwtService(OTHER_SECRET, "gativah-admin", 1_800_000);
        String token = issuer.generate(user(), AdminRole.MODERATOR.authorities());

        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(Exception.class);
    }

    @Test
    void rejects_a_token_with_the_wrong_audience() {
        AdminJwtService issuer = new AdminJwtService(SECRET, "some-other-aud", 1_800_000);
        AdminJwtService verifier = new AdminJwtService(SECRET, "gativah-admin", 1_800_000);
        String token = issuer.generate(user(), AdminRole.MODERATOR.authorities());

        assertThatThrownBy(() -> verifier.parse(token)).isInstanceOf(IllegalArgumentException.class);
    }
}

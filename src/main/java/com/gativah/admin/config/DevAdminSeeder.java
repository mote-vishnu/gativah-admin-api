package com.gativah.admin.config;

import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Dev-only: seed a first SUPER_ADMIN so the console is usable locally. Runs only
 * when the admin_user table is empty. Never active outside the dev profile.
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    private final AdminUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public DevAdminSeeder(AdminUserRepository repo, PasswordEncoder passwordEncoder,
                          @Value("${admin.seed.email:admin@gativah.com}") String seedEmail,
                          @Value("${admin.seed.password:ChangeMe!123}") String seedPassword) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) {
            return;
        }
        AdminUser u = new AdminUser();
        u.setEmail(seedEmail);
        u.setName("Root Admin");
        u.setRole(AdminRole.SUPER_ADMIN);
        u.setStatus(AdminUser.STATUS_ACTIVE);
        u.setPasswordHash(passwordEncoder.encode(seedPassword));
        u.setMfaEnrolled(false);
        repo.save(u);
        log.warn("Seeded dev SUPER_ADMIN '{}' — change the password immediately.", seedEmail);
    }
}

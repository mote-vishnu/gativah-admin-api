package com.gativah.admin.staff.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.auth.model.AdminRole;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminRoleRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.staff.dto.AdminLite;
import com.gativah.admin.staff.dto.InviteStaffRequest;
import com.gativah.admin.staff.dto.StaffRow;
import com.gativah.admin.staff.dto.UpdateStaffRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffServiceImpl implements StaffService {

    private final AdminUserRepository repo;
    private final AdminRoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public StaffServiceImpl(AdminUserRepository repo, AdminRoleRepository roleRepo,
                            PasswordEncoder passwordEncoder, AuditService audit) {
        this.repo = repo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffRow> list(Pageable pageable) {
        return repo.findAllByOrderByCreatedAtDesc(pageable).map(StaffServiceImpl::toRow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminLite> directory() {
        return repo.findAll().stream().map(u -> new AdminLite(u.getId(), u.getName())).toList();
    }

    @Override
    @Transactional
    public StaffRow invite(Long actorAdminId, InviteStaffRequest req) {
        if (repo.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        Set<AdminRole> roles = resolveRoles(req.roleIds());
        AdminUser u = new AdminUser();
        u.setEmail(req.email().trim());
        u.setName(req.name().trim());
        u.setRoles(roles);
        u.setStatus(AdminUser.STATUS_ACTIVE);
        u.setMfaEnrolled(false);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u = repo.save(u);
        audit.record(actorAdminId, "STAFF_INVITE", "ADMIN", String.valueOf(u.getId()),
                req.email() + " as " + String.join(",", u.roleNames()), null, null);
        return toRow(u);
    }

    @Override
    @Transactional
    public StaffRow update(Long actorAdminId, Long id, UpdateStaffRequest req) {
        AdminUser u = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found: " + id));
        if (req.status() != null) {
            // Disabling (or any status change away from ACTIVE) forces logout of live tokens.
            if (!req.status().equals(u.getStatus()) && !AdminUser.STATUS_ACTIVE.equals(req.status())) {
                u.setTokenVersion(u.getTokenVersion() + 1);
            }
            u.setStatus(req.status());
        }
        u = repo.save(u);
        audit.record(actorAdminId, "STAFF_UPDATE", "ADMIN", String.valueOf(id),
                "status=" + u.getStatus(), null, null);
        return toRow(u);
    }

    @Override
    @Transactional
    public StaffRow setRoles(Long actorAdminId, Long id, List<Long> roleIds) {
        AdminUser u = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found: " + id));
        Set<AdminRole> roles = resolveRoles(roleIds);
        u.setRoles(roles);
        u = repo.save(u);
        audit.record(actorAdminId, "STAFF_SET_ROLES", "ADMIN", String.valueOf(id),
                "roles=" + String.join(",", u.roleNames()), null, null);
        return toRow(u);
    }

    private Set<AdminRole> resolveRoles(List<Long> roleIds) {
        Set<AdminRole> roles = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            AdminRole role = roleRepo.findById(roleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + roleId));
            roles.add(role);
        }
        return roles;
    }

    private static StaffRow toRow(AdminUser u) {
        return new StaffRow(u.getId(), u.getEmail(), u.getName(), u.roleNames(),
                u.getStatus(), u.isMfaEnrolled(), u.getLastLoginAt(), u.getCreatedAt());
    }
}

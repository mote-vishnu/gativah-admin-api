package com.gativah.admin.auth.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Platform operator account (admin_user, V87). Access derives from assigned roles (V89). */
@Entity
@Table(name = "admin_user")
@Getter
@Setter
@NoArgsConstructor
public class AdminUser {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISABLED = "DISABLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "admin_user_role",
            joinColumns = @JoinColumn(name = "admin_user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<AdminRole> roles = new HashSet<>();

    @Column(nullable = false)
    private String status = STATUS_ACTIVE;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "mfa_enrolled", nullable = false)
    private boolean mfaEnrolled;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** Assigned role names, sorted. */
    public List<String> roleNames() {
        return roles.stream().map(AdminRole::getName).sorted().toList();
    }

    /** Effective permission codes (union across all roles), sorted & de-duped. */
    public List<String> permissionCodes() {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(AdminPermission::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

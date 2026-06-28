package com.gativah.admin.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A granular capability = feature × action (admin_permission, V89). The
 * {@code code} ("FINANCE:VIEW") is what flows into JWT authorities / @PreAuthorize.
 */
@Entity
@Table(name = "admin_permission")
@Getter
@Setter
@NoArgsConstructor
public class AdminPermission {

    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_ADD = "ADD";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_DELETE = "DELETE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "feature_id", nullable = false)
    private AdminFeature feature;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false, unique = true)
    private String code;
}

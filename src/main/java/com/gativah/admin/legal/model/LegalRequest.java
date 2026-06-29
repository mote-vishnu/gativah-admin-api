package com.gativah.admin.legal.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A formal legal request — subpoena, court order, preservation hold, … (legal_request, V90). */
@Entity
@Table(name = "legal_request")
@Getter
@Setter
@NoArgsConstructor
public class LegalRequest {

    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_UNDER_REVIEW = "UNDER_REVIEW";
    public static final String STATUS_ACTIONED = "ACTIONED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The legal-process identifier (court/case ref) — mandatory; disclosures hang off this. */
    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "requesting_authority", nullable = false)
    private String requestingAuthority;

    @Column(name = "subject_user_id")
    private Long subjectUserId;

    @Column(length = 2000)
    private String scope;

    @Column(nullable = false)
    private String status = STATUS_RECEIVED;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(length = 2000)
    private String notes;

    @Column(name = "approval_status", nullable = false)
    private String approvalStatus = APPROVAL_PENDING;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approval_note", length = 1000)
    private String approvalNote;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (receivedAt == null) {
            receivedAt = now;
        }
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

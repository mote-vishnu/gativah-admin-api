package com.gativah.admin.moderation.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A user's appeal against a sanction/takedown (appeal, V88). */
@Entity
@Table(name = "appeal")
@Getter
@Setter
@NoArgsConstructor
public class Appeal {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_GRANTED = "GRANTED";
    public static final String STATUS_DENIED = "DENIED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_user_id", nullable = false)
    private Long subjectUserId;

    @Column(name = "related_report_id")
    private Long relatedReportId;

    @Column(name = "related_action_id")
    private Long relatedActionId;

    @Column(length = 2000)
    private String message;

    @Column(nullable = false)
    private String status = STATUS_OPEN;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = STATUS_OPEN;
        }
    }
}

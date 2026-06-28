package com.gativah.admin.moderation.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * User-submitted content report (content_report, owned by pacegrit-service —
 * V33). The admin API reads it and updates its review fields on resolve.
 */
@Entity
@Table(name = "content_report")
@Getter
@Setter
@NoArgsConstructor
public class ContentReport {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVIEWING = "REVIEWING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_DISMISSED = "DISMISSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_user_id")
    private Long reporterUserId;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "content_type")
    private String contentType;

    private String reason;

    private String details;

    private String status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

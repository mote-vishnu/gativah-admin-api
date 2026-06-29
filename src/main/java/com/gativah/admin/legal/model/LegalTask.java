package com.gativah.admin.legal.model;

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

/** A task on a legal case (legal_task, V96). */
@Entity
@Table(name = "legal_task")
@Getter
@Setter
@NoArgsConstructor
public class LegalTask {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false)
    private String status = STATUS_OPEN;

    @Column(name = "assignee_admin_id")
    private Long assigneeAdminId;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

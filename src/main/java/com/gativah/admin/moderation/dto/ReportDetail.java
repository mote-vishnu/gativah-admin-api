package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

/** Full report view with the reported content inlined + author moderation status. */
public record ReportDetail(
        Long id,
        String contentType,
        Long contentId,
        String reason,
        String details,
        String status,
        LocalDateTime createdAt,
        Long reporterUserId,
        String reporterUsername,
        Long authorUserId,
        String authorUsername,
        String authorStatus,
        String snippet,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long assigneeAdminId) {
}

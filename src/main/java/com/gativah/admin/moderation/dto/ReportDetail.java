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
        long reporterCount,
        Long authorUserId,
        String authorUsername,
        String authorDisplayName,
        String authorPhotoUrl,
        String authorStatus,
        String privacy,
        int mediaCount,
        String snippet,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long assigneeAdminId) {
}

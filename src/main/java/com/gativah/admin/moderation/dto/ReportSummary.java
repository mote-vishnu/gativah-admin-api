package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

/** A row in the grievance queue — report joined to reporter, author, and content snippet. */
public record ReportSummary(
        Long id,
        String contentType,
        Long contentId,
        String reason,
        String status,
        LocalDateTime createdAt,
        Long reporterUserId,
        String reporterUsername,
        Long authorUserId,
        String authorUsername,
        String snippet) {
}

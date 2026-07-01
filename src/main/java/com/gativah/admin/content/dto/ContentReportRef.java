package com.gativah.admin.content.dto;

import java.time.LocalDateTime;

/** A moderation report filed against a piece of content — links into Grievances. */
public record ContentReportRef(
        Long reportId,
        String reason,
        Long reporterUserId,
        String reporterUsername,
        String status,
        LocalDateTime createdAt) {
}

package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

/** A report filed against this user's content, for the Reports &amp; sanctions tab. */
public record UserReportRow(
        Long reportId,
        String contentType,
        Long contentId,
        String snippet,
        String reason,
        String status,
        LocalDateTime createdAt) {
}

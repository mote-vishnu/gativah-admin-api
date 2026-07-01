package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;

/** A reported post/comment inside a club — links into Grievances via {@code latestReportId}. */
public record ClubReportedContent(
        String contentType,
        Long contentId,
        String snippet,
        Long authorUserId,
        String authorUsername,
        long openReports,
        long totalReports,
        Long latestReportId,
        LocalDateTime latestReportAt) {
}

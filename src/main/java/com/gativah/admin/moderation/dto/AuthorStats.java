package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

/** Aggregated standing/reach for a content author (read-side rollup). */
public record AuthorStats(
        String accountStatus,
        long reportsAgainst,
        long openReports,
        long followers,
        String plan,
        LocalDateTime memberSince) {
}

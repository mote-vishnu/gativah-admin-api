package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Context on the reported content's author: standing, reach, and sanction history. */
public record AuthorHistory(
        Long authorUserId,
        String accountStatus,
        long reportsAgainst,
        long openReports,
        long followers,
        String plan,
        LocalDateTime memberSince,
        List<AuthorSanction> recentSanctions) {
}

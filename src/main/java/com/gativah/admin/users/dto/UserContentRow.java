package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

/** One piece of a user's content (post or comment) for the profile Content tab. */
public record UserContentRow(
        String type,
        Long id,
        String snippet,
        String kind,
        LocalDateTime createdAt,
        boolean removed,
        long views,
        long openReports) {
}

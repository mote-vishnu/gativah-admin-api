package com.gativah.admin.content.dto;

import java.time.LocalDateTime;

public record StoryRow(
        Long id,
        Long authorUserId,
        String authorUsername,
        String kind,
        String snippet,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        long viewCount,
        long reactionCount,
        boolean removed) {
}

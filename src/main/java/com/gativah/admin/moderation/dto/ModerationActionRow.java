package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

public record ModerationActionRow(
        Long id,
        Long reportId,
        Long adminUserId,
        String targetType,
        Long targetId,
        String action,
        String reason,
        LocalDateTime createdAt) {
}

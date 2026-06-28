package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

public record AppealRow(
        Long id,
        Long subjectUserId,
        Long relatedReportId,
        Long relatedActionId,
        String message,
        String status,
        LocalDateTime createdAt) {
}

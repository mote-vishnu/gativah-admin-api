package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

public record AppealRow(
        Long id,
        Long subjectUserId,
        String subjectUsername,
        Long relatedReportId,
        Long relatedActionId,
        String originalAction,
        String message,
        String status,
        LocalDateTime createdAt) {
}

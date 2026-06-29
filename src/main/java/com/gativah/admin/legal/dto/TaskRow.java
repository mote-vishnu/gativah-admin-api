package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record TaskRow(
        Long id,
        String title,
        String status,
        Long assigneeAdminId,
        LocalDateTime dueAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}

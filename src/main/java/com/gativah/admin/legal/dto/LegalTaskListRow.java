package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

/** An open task surfaced on the cross-request compliance worklist. */
public record LegalTaskListRow(
        Long id,
        Long requestId,
        String reference,
        String requestType,
        String title,
        String status,
        Long assigneeAdminId,
        LocalDateTime dueAt,
        LocalDateTime createdAt,
        boolean overdue) {
}

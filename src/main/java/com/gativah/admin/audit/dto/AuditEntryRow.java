package com.gativah.admin.audit.dto;

import java.time.LocalDateTime;

public record AuditEntryRow(
        Long id,
        Long adminUserId,
        String action,
        String targetType,
        String targetId,
        String summary,
        String ip,
        LocalDateTime createdAt) {
}

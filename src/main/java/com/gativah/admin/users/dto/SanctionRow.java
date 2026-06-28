package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

public record SanctionRow(
        Long id,
        String type,
        String reason,
        LocalDateTime suspendedUntil,
        Long adminUserId,
        LocalDateTime createdAt) {
}

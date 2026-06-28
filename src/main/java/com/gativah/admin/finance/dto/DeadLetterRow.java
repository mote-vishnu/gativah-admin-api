package com.gativah.admin.finance.dto;

import java.time.LocalDateTime;

public record DeadLetterRow(
        Long id,
        String platform,
        String eventType,
        int attempts,
        String lastError,
        LocalDateTime receivedAt) {
}

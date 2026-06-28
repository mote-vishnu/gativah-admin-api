package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record LegalRequestSummary(
        Long id,
        String reference,
        String requestType,
        String requestingAuthority,
        Long subjectUserId,
        String status,
        LocalDateTime receivedAt,
        LocalDateTime dueAt,
        long disclosureCount) {
}

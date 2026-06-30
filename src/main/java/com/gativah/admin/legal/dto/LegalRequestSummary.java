package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record LegalRequestSummary(
        Long id,
        String reference,
        String requestType,
        String requestingAuthority,
        Long subjectUserId,
        String status,
        String approvalStatus,
        LocalDateTime receivedAt,
        LocalDateTime dueAt,
        boolean overdue,
        long disclosureCount) {
}

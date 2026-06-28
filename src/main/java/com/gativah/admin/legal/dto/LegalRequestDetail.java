package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record LegalRequestDetail(
        Long id,
        String reference,
        String requestType,
        String requestingAuthority,
        Long subjectUserId,
        String scope,
        String status,
        LocalDateTime receivedAt,
        LocalDateTime dueAt,
        String notes,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DisclosureRow> disclosures) {
}

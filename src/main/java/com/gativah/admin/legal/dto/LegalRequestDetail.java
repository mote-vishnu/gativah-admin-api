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
        String approvalStatus,
        Long approvedBy,
        LocalDateTime approvedAt,
        String approvalNote,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DisclosureRow> disclosures,
        List<TaskRow> tasks,
        List<CorrespondenceRow> correspondence,
        List<CustodyEventRow> custody) {
}

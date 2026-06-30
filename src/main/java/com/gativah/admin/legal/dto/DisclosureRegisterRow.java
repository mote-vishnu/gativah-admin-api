package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

/** A disclosure entry on the global, auditable disclosure register. */
public record DisclosureRegisterRow(
        Long id,
        Long requestId,
        String reference,
        String requestType,
        Long disclosedBy,
        String recipient,
        String dataCategories,
        String justification,
        LocalDateTime disclosedAt) {
}

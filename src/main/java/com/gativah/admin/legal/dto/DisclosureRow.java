package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record DisclosureRow(
        Long id,
        Long disclosedBy,
        String recipient,
        String dataCategories,
        String justification,
        LocalDateTime disclosedAt) {
}

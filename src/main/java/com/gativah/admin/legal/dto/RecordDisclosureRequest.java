package com.gativah.admin.legal.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordDisclosureRequest(
        @NotBlank String recipient,
        @NotBlank String dataCategories,
        @NotBlank String justification) {
}

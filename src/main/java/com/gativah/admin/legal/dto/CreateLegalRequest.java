package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record CreateLegalRequest(
        @NotBlank String reference,
        @NotBlank String requestType,
        @NotBlank String requestingAuthority,
        Long subjectUserId,
        String scope,
        LocalDateTime dueAt,
        String notes) {
}

package com.gativah.admin.billing.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Grant a complimentary entitlement. expiresAt null = no expiry. */
public record GrantCompRequest(
        @NotNull Long userId,
        @NotBlank String code,
        LocalDateTime expiresAt,
        String reason) {
}

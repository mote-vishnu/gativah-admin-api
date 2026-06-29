package com.gativah.admin.billing.dto;

import java.time.LocalDateTime;

/** A row in the entitlement registry. source = COMP | SUBSCRIPTION. */
public record EntitlementRow(
        Long id,
        Long userId,
        String username,
        String code,
        String name,
        boolean active,
        String source,
        LocalDateTime expiresAt,
        LocalDateTime updatedAt) {
}

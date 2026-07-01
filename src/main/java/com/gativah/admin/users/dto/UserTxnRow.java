package com.gativah.admin.users.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One billing transaction row on the profile Billing tab. */
public record UserTxnRow(
        Long id,
        String type,
        String status,
        BigDecimal amount,
        String currency,
        String platform,
        LocalDateTime purchasedAt) {
}

package com.gativah.admin.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundRow(
        Long id,
        Long userId,
        String username,
        String planCode,
        String type,
        BigDecimal grossAmount,
        String grossCurrency,
        String countryCode,
        LocalDateTime purchasedAt) {
}

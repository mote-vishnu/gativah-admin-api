package com.gativah.admin.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRow(
        Long id,
        Long userId,
        String planCode,
        String platform,
        String type,
        String status,
        BigDecimal grossAmount,
        String grossCurrency,
        String countryCode,
        String source,
        LocalDateTime purchasedAt) {
}

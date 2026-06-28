package com.gativah.admin.finance.dto;

import java.time.LocalDateTime;

public record SubscriptionRow(
        Long id,
        Long userId,
        String planCode,
        String platform,
        String state,
        boolean autoRenew,
        boolean trial,
        LocalDateTime currentPeriodEnd) {
}

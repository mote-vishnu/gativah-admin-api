package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

public record SubscriptionInfo(
        String planCode,
        String platform,
        String state,
        boolean trial,
        boolean autoRenew,
        LocalDateTime currentPeriodEnd) {
}

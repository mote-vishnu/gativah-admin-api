package com.gativah.admin.users.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetail(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String photoUrl,
        boolean verified,
        String accountStatus,
        LocalDateTime suspendedUntil,
        String statusReason,
        LocalDateTime statusChangedAt,
        LocalDateTime createdAt,
        SubscriptionInfo subscription,
        List<SanctionRow> sanctions) {
}

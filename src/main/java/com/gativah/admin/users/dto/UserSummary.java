package com.gativah.admin.users.dto;

import java.time.LocalDateTime;

/** A row in the user directory. */
public record UserSummary(
        Long id,
        String username,
        String email,
        String fullName,
        String accountStatus,
        boolean verified,
        String subscriptionState,
        LocalDateTime createdAt) {
}

package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;

public record ClubSummary(
        Long id,
        String name,
        Long ownerUserId,
        String ownerUsername,
        String visibility,
        int memberCount,
        long eventCount,
        boolean removed,
        LocalDateTime createdAt) {
}

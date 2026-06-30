package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClubDetail(
        Long id,
        String name,
        String description,
        String photoUrl,
        Long ownerUserId,
        String ownerUsername,
        String visibility,
        int memberCount,
        boolean removed,
        LocalDateTime createdAt,
        ClubInsights insights,
        List<ClubMemberRow> members,
        List<ClubEventRow> events) {
}

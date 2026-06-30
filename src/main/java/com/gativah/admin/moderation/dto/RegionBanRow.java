package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

/** A geo-restriction on a post (post_region_ban), for the Region bans screen. */
public record RegionBanRow(
        Long id,
        Long postId,
        String country,
        String reason,
        Long bannedByAdminId,
        LocalDateTime bannedAt,
        boolean lifted,
        String authorUsername,
        String snippet) {
}

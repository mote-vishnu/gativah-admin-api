package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;

public record ClubMemberRow(
        Long userId,
        String username,
        String role,
        String status,
        LocalDateTime joinedAt) {
}

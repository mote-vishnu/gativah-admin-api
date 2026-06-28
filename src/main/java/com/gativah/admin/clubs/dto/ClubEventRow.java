package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;

public record ClubEventRow(
        Long id,
        String title,
        String kind,
        LocalDateTime startsAt,
        long rsvpCount,
        boolean removed) {
}

package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;

/** One attendee's RSVP to a club event. {@code status} = GOING | MAYBE | DECLINED. */
public record RsvpRow(Long userId, String username, String status, LocalDateTime respondedAt) {
}

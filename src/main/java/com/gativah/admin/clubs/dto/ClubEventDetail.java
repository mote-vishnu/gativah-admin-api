package com.gativah.admin.clubs.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Read-only drill-down for a single club event: fields + RSVP tallies/list + route. */
public record ClubEventDetail(
        Long id,
        Long clubId,
        String title,
        String kind,
        String description,
        String location,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer distanceM,
        Long createdByUserId,
        String createdByUsername,
        LocalDateTime createdAt,
        boolean removed,
        long rsvpGoing,
        long rsvpMaybe,
        long rsvpDeclined,
        List<RsvpRow> rsvps,
        List<RoutePoint> route) {
}

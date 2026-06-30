package com.gativah.admin.clubs.dto;

/** Per-club aggregate insights surfaced on the club detail screen. */
public record ClubInsights(
        long owners,
        long admins,
        long regularMembers,
        long pendingMembers,
        long upcomingEvents,
        long pastEvents,
        long totalRsvps,
        long newMembers30d) {
}

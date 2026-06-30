package com.gativah.admin.clubs.dto;

/** Directory-wide KPI band for the Clubs overview. */
public record ClubStats(
        long totalClubs,
        long activeClubs,
        long removedClubs,
        long privateClubs,
        long totalMembers,
        double avgMembers,
        long newClubs30d,
        long upcomingEvents,
        long largestClubMembers) {
}

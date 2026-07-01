package com.gativah.admin.analytics.dto;

/**
 * Headline KPIs for the analytics overview. {@code dau}/{@code wau}/{@code mau} are
 * point-in-time active-user snapshots; {@code stickiness} = DAU/MAU × 100. The
 * period totals ({@code activeUsers}, {@code newSignups}, {@code totalEvents}) each
 * carry a delta (%) versus the immediately preceding window of the same length.
 */
public record OverviewKpis(
        long dau,
        long wau,
        long mau,
        double stickiness,
        long activeUsers,
        double activeUsersDelta,
        long newSignups,
        double newSignupsDelta,
        long totalEvents,
        double totalEventsDelta,
        double avgEventsPerUser) {
}

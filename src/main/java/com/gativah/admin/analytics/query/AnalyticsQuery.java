package com.gativah.admin.analytics.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.gativah.admin.analytics.dto.CohortSize;
import com.gativah.admin.analytics.dto.CountryCount;
import com.gativah.admin.analytics.dto.EventBreakdownRow;
import com.gativah.admin.analytics.dto.PlatformRow;
import com.gativah.admin.analytics.dto.RetentionCell;
import com.gativah.admin.analytics.dto.StickinessPoint;
import com.gativah.admin.analytics.dto.TimePoint;
import com.gativah.admin.analytics.dto.VersionRow;

/** Read-only aggregation over the analytics_event sink (V80) + user_account. */
public interface AnalyticsQuery {

    long distinctActiveUsers(LocalDateTime from, LocalDateTime to);

    long countEvents(LocalDateTime from, LocalDateTime to);

    long countSignups(LocalDateTime from, LocalDateTime to);

    /** Zero-filled daily distinct active users across [from, to]. */
    List<TimePoint> activeUsersByDay(LocalDate from, LocalDate to);

    /** Zero-filled daily signups across [from, to]. */
    List<TimePoint> signupsByDay(LocalDate from, LocalDate to);

    /** Per-event volume (pct left 0 — filled by the service against the grand total). */
    List<EventBreakdownRow> eventBreakdown(LocalDateTime from, LocalDateTime to);

    /** Daily DAU / trailing-30-day MAU / stickiness across [from, to]. */
    List<StickinessPoint> stickinessByDay(LocalDate from, LocalDate to);

    /** Per-platform volume (pct left 0 — filled by the service). */
    List<PlatformRow> platformBreakdown(LocalDateTime from, LocalDateTime to);

    List<VersionRow> versionBreakdown(LocalDateTime from, LocalDateTime to);

    /** Distinct users who fired each of {@code events} in the window. */
    Map<String, Long> funnelUsers(LocalDateTime from, LocalDateTime to, List<String> events);

    /** Signup-cohort sizes (by week) for users created since {@code from}. */
    List<CohortSize> cohortSizes(LocalDateTime from);

    /** Retention cells (cohort week × week offset) for users created since {@code from}. */
    List<RetentionCell> retentionCells(LocalDateTime from);

    /** Distinct users grouped by their resolved country (ISO alpha-2). */
    List<CountryCount> geoByCountry();

    /** Total user_account rows (for mapped-vs-total coverage). */
    long totalUsers();
}

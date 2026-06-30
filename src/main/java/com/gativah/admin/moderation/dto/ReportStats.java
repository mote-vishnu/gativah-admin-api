package com.gativah.admin.moderation.dto;

/** Queue health for the grievance KPI band. */
public record ReportStats(
        long open,
        long pending,
        long reviewing,
        long slaBreaches,
        long resolved24h,
        long repeatOffenders) {
}

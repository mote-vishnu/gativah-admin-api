package com.gativah.admin.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * One signup-cohort row of the retention grid. {@code retained}/{@code retainedPct}
 * are indexed by week offset from the cohort week (index 0 = signup week).
 */
public record RetentionRow(
        LocalDate cohortWeek,
        long cohortSize,
        List<Long> retained,
        List<Double> retainedPct) {
}

package com.gativah.admin.analytics.dto;

import java.util.List;

/** Weekly signup-cohort retention grid: {@code weeks} columns per cohort row. */
public record RetentionResponse(int weeks, List<RetentionRow> cohorts) {
}

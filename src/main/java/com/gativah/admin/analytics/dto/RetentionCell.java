package com.gativah.admin.analytics.dto;

import java.time.LocalDate;

/** Raw retention datum: distinct cohort users active {@code weekOffset} weeks after signup. */
public record RetentionCell(LocalDate cohortWeek, int weekOffset, long retained) {
}

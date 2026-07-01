package com.gativah.admin.analytics.dto;

import java.time.LocalDate;

/** Raw signup-cohort size (users who signed up in a given week). */
public record CohortSize(LocalDate cohortWeek, long size) {
}

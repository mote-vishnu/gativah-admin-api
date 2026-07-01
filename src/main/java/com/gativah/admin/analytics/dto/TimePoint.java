package com.gativah.admin.analytics.dto;

import java.time.LocalDate;

/** A single (day, value) point in a daily time series. */
public record TimePoint(LocalDate date, long value) {
}

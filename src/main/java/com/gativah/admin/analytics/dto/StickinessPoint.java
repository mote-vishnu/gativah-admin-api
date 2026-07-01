package com.gativah.admin.analytics.dto;

import java.time.LocalDate;

/** Daily engagement: DAU, trailing-30-day MAU, and stickiness = DAU/MAU × 100. */
public record StickinessPoint(LocalDate date, long dau, long mau, double stickiness) {
}

package com.gativah.admin.content.dto;

import java.math.BigDecimal;
import java.util.List;

/** The workout/activity summary attached to an activity post (like the app's activity card). */
public record ActivityShare(
        String activityType,
        BigDecimal distanceKm,
        Integer durationSecs,
        BigDecimal paceMinPerKm,
        Integer caloriesBurned,
        List<GeoPoint> route) {
}

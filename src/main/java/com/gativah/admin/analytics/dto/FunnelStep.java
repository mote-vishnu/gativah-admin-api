package com.gativah.admin.analytics.dto;

/**
 * One step of the activation funnel: distinct users who fired the step's event in
 * the window, plus conversion (%) from the previous step and from the first step.
 */
public record FunnelStep(
        String key,
        String label,
        long users,
        double conversionFromPrev,
        double conversionFromStart) {
}

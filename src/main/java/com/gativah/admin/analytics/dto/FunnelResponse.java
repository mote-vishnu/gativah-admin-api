package com.gativah.admin.analytics.dto;

import java.util.List;

/** Activation funnel: app_open → workout_viewed → workout_started → workout_completed. */
public record FunnelResponse(List<FunnelStep> steps) {
}

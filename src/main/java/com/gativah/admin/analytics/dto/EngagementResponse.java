package com.gativah.admin.analytics.dto;

import java.util.List;

/** Daily stickiness series (DAU / trailing-30-day MAU) for the engagement chart. */
public record EngagementResponse(List<StickinessPoint> series) {
}

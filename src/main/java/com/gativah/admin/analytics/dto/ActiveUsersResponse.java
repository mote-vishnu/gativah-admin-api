package com.gativah.admin.analytics.dto;

import java.util.List;

/** Daily active-user and new-signup series over the requested window. */
public record ActiveUsersResponse(List<TimePoint> activeUsers, List<TimePoint> newSignups) {
}

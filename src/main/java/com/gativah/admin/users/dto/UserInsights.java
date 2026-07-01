package com.gativah.admin.users.dto;

import java.util.List;

/** Derived risk + social + activity context for a user profile. riskLevel = LOW | MEDIUM | HIGH. */
public record UserInsights(
        long reportsAgainst,
        long sanctionCount,
        long followers,
        long following,
        long posts,
        int riskScore,
        String riskLevel,
        List<DeviceRow> devices,
        List<ActivityPoint> activity) {
}

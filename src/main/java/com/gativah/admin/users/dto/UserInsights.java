package com.gativah.admin.users.dto;

import java.util.List;

/** Derived risk + activity context for a user profile. riskLevel = LOW | MEDIUM | HIGH. */
public record UserInsights(
        long reportsAgainst,
        long sanctionCount,
        int riskScore,
        String riskLevel,
        List<DeviceRow> devices,
        List<ActivityPoint> activity) {
}

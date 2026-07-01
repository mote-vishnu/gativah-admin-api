package com.gativah.admin.analytics.dto;

/** Event + user volume for one client platform (ios / android / web / unknown). */
public record PlatformRow(String platform, long events, long users, double pct) {
}

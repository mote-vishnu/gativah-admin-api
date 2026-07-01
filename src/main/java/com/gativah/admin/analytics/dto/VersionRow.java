package com.gativah.admin.analytics.dto;

/** Event + user volume for one app version string. */
public record VersionRow(String appVersion, long events, long users) {
}

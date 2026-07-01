package com.gativah.admin.analytics.dto;

/** Raw per-country distinct-user count (ISO alpha-2), before name/share enrichment. */
public record CountryCount(String code, long users) {
}

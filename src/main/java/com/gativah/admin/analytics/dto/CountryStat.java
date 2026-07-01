package com.gativah.admin.analytics.dto;

/** Distinct users located in one country. {@code code} is ISO-3166 alpha-2. */
public record CountryStat(String code, String name, long users, double pct) {
}

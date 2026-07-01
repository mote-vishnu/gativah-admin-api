package com.gativah.admin.analytics.dto;

import java.util.List;

/**
 * User distribution by country for the world map. {@code mappedUsers} is how many
 * of {@code totalUsers} have a resolvable country.
 */
public record GeoResponse(long totalUsers, long mappedUsers, List<CountryStat> countries) {
}

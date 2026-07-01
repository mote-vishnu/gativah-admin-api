package com.gativah.admin.analytics.dto;

import java.util.List;

/** Platform split (donut) plus the app-version table over the window. */
public record PlatformResponse(List<PlatformRow> platforms, List<VersionRow> versions) {
}

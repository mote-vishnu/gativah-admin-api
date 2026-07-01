package com.gativah.admin.analytics.dto;

import java.util.List;

/** Event-name breakdown over the window, with the grand total for share math. */
public record EventBreakdownResponse(long total, List<EventBreakdownRow> events) {
}

package com.gativah.admin.analytics.dto;

/** One event name's volume: total fires, distinct users, and share of all events (%). */
public record EventBreakdownRow(String name, long count, long uniqueUsers, double pct) {
}

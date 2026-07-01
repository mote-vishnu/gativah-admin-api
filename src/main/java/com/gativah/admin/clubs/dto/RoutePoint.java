package com.gativah.admin.clubs.dto;

/** A point on a club event's planned route (ordered by seqNo). */
public record RoutePoint(int seqNo, double lat, double lng) {
}

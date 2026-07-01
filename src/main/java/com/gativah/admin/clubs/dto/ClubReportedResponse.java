package com.gativah.admin.clubs.dto;

import java.util.List;

/** Wrapper so the controller doesn't return a bare List. */
public record ClubReportedResponse(List<ClubReportedContent> items) {
}

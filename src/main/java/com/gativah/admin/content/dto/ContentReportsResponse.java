package com.gativah.admin.content.dto;

import java.util.List;

/** Wrapper so the controller doesn't return a bare List. */
public record ContentReportsResponse(List<ContentReportRef> reports) {
}

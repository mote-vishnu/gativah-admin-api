package com.gativah.admin.security.dto;

import java.util.List;

/** Wrapper so the controller doesn't return a bare List. */
public record ActiveSessionsResponse(List<ActiveSessionRow> sessions) {
}

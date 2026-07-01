package com.gativah.admin.users.dto;

import java.util.List;

/** Reports filed against this user's content. */
public record UserReportsResponse(List<UserReportRow> items) {
}

package com.gativah.admin.staff.dto;

import java.time.LocalDateTime;

/** One admin session (issued token) for the Staff sessions view. */
public record SessionRow(Long id, String ip, String userAgent, LocalDateTime createdAt, boolean revoked) {
}

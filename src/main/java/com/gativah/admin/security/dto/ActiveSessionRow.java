package com.gativah.admin.security.dto;

import java.time.LocalDateTime;

/** An active (non-revoked) admin session, enriched with the operator's identity. */
public record ActiveSessionRow(
        Long sessionId,
        Long adminUserId,
        String adminName,
        String email,
        String ip,
        String userAgent,
        LocalDateTime createdAt) {
}

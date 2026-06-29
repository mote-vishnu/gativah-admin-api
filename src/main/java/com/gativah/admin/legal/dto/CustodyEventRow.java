package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record CustodyEventRow(
        Long id,
        String event,
        String detail,
        Long actorAdminId,
        LocalDateTime createdAt) {
}

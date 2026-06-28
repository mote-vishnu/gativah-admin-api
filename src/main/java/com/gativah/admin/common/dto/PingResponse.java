package com.gativah.admin.common.dto;

/** Liveness payload for {@code GET /api/v1/ping}. */
public record PingResponse(String service, String status) {
}

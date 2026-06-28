package com.gativah.admin.content.dto;

import java.time.LocalDateTime;

/** A post or comment in the content browser. {@code type} is POST | COMMENT. */
public record ContentRow(
        Long id,
        String type,
        Long authorUserId,
        String authorUsername,
        String snippet,
        LocalDateTime createdAt,
        boolean removed) {
}

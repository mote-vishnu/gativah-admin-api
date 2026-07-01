package com.gativah.admin.content.dto;

import java.time.LocalDateTime;

/** A comment shown under a post in the content drill-down. */
public record ContentCommentRow(
        Long id,
        Long authorUserId,
        String authorUsername,
        String content,
        LocalDateTime createdAt,
        boolean removed) {
}

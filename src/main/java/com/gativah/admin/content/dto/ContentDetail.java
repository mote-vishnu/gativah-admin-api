package com.gativah.admin.content.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full drill-down for a post or comment — mirrors what the Gativah app shows:
 * body, media, reaction breakdown and comments (post) or parent context (comment).
 */
public record ContentDetail(
        String type,
        Long id,
        Long authorUserId,
        String authorUsername,
        String content,
        LocalDateTime createdAt,
        boolean removed,
        String kind,
        String privacy,
        long viewCount,
        Long parentPostId,
        String parentSnippet,
        long reactionTotal,
        List<ReactionCount> reactions,
        List<MediaItem> media,
        long commentCount,
        List<ContentCommentRow> comments,
        ActivityShare activity) {
}

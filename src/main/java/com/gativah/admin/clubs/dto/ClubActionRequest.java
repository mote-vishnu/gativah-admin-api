package com.gativah.admin.clubs.dto;

/** Optional rationale for a club moderation action (remove club / remove event). */
public record ClubActionRequest(String reason) {
}

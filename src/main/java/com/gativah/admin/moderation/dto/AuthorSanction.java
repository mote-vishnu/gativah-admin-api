package com.gativah.admin.moderation.dto;

import java.time.LocalDateTime;

public record AuthorSanction(String type, String reason, LocalDateTime suspendedUntil, LocalDateTime createdAt) {
}

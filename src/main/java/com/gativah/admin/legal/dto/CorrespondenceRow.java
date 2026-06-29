package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

public record CorrespondenceRow(
        Long id,
        String direction,
        String channel,
        String summary,
        Long createdBy,
        LocalDateTime createdAt) {
}

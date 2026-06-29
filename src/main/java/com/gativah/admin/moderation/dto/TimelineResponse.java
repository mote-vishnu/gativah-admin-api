package com.gativah.admin.moderation.dto;

import java.util.List;

public record TimelineResponse(List<ModerationActionRow> items) {
}

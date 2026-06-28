package com.gativah.admin.moderation.dto;

import java.util.List;

public record BulkResolveResponse(int resolved, int failed, List<Long> failedIds) {
}

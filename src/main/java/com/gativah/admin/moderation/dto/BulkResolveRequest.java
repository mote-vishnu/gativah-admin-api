package com.gativah.admin.moderation.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Apply the same decision to many reports at once. {@code reason} is shared across all. */
public record BulkResolveRequest(
        @NotEmpty List<Long> ids,
        @NotNull ResolveAction action,
        String reason) {
}

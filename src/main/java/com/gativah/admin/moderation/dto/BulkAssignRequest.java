package com.gativah.admin.moderation.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Assign many reports at once. {@code adminId} null = unassign all. */
public record BulkAssignRequest(@NotEmpty List<Long> ids, Long adminId) {
}

package com.gativah.admin.moderation.dto;

import jakarta.validation.constraints.NotNull;

/** {@code suspendDays} applies to SUSPEND (defaults to 7 when null). */
public record ResolveRequest(@NotNull ResolveAction action, String reason, Integer suspendDays) {
}

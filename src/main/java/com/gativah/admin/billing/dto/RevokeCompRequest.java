package com.gativah.admin.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RevokeCompRequest(@NotNull Long userId, @NotBlank String code) {
}

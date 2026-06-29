package com.gativah.admin.legal.dto;

import jakarta.validation.constraints.NotBlank;

/** direction = INBOUND | OUTBOUND | NOTE. */
public record AddCorrespondenceRequest(@NotBlank String direction, String channel, @NotBlank String summary) {
}

package com.gativah.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaEnableRequest(@NotBlank String code) {
}

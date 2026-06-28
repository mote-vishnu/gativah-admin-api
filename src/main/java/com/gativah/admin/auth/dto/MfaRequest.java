package com.gativah.admin.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MfaRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String code) {
}

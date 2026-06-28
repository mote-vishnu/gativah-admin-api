package com.gativah.admin.auth.dto;

/** Enrollment payload: the base32 secret (manual entry) + otpauth URI (QR). */
public record MfaStartResponse(String secret, String otpauthUri, boolean alreadyEnrolled) {
}

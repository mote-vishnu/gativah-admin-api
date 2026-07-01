package com.gativah.admin.security.dto;

/** An active admin who has not yet enrolled MFA (a posture gap). */
public record UnenrolledAdmin(Long id, String name, String email) {
}

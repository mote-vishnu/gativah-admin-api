package com.gativah.admin.users.dto;

/** Suspend a user. {@code days} defaults to 7 when null. */
public record SuspendRequest(String reason, Integer days) {
}

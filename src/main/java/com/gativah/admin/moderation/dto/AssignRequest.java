package com.gativah.admin.moderation.dto;

/** Assign a report to an operator. {@code adminId} null = unassign. */
public record AssignRequest(Long adminId) {
}

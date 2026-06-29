package com.gativah.admin.staff.dto;

/** Minimal admin identity for resolving actor ids to names in logs/assignees. */
public record AdminLite(Long id, String name) {
}

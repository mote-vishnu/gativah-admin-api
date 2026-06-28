package com.gativah.admin.roles.dto;

import java.util.List;

/** Partial update — null fields are left unchanged. {@code permissionIds} replaces the set when present. */
public record UpdateRoleRequest(
        String name,
        String description,
        List<Long> permissionIds) {
}

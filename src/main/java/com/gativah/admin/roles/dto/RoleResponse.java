package com.gativah.admin.roles.dto;

import java.util.List;

public record RoleResponse(
        Long id,
        String name,
        String description,
        boolean system,
        List<Long> permissionIds,
        List<String> permissions,
        long userCount) {
}

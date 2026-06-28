package com.gativah.admin.roles.dto;

import java.util.List;

/** All permissions belonging to one feature, for the role-editor matrix. */
public record FeaturePermissions(
        String featureCode,
        String label,
        int sortOrder,
        List<PermissionResponse> permissions) {
}

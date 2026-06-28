package com.gativah.admin.roles.dto;

import java.util.List;

/** The full feature × action catalog, grouped by feature. */
public record PermissionCatalogResponse(List<FeaturePermissions> features) {
}

package com.gativah.admin.roles.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoleRequest(
        @NotBlank String name,
        String description,
        @NotNull List<Long> permissionIds) {
}

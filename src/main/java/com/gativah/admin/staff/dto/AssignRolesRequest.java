package com.gativah.admin.staff.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AssignRolesRequest(@NotNull List<Long> roleIds) {
}

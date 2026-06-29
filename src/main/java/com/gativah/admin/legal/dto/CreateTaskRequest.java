package com.gativah.admin.legal.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(@NotBlank String title, Long assigneeAdminId, LocalDateTime dueAt) {
}

package com.gativah.admin.staff.dto;

import java.time.LocalDateTime;
import java.util.List;

public record StaffRow(
        Long id,
        String email,
        String name,
        List<String> roles,
        String status,
        boolean mfaEnrolled,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {
}

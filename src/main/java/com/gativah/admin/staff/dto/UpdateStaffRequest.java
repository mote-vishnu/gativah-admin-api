package com.gativah.admin.staff.dto;

/** Update account status only. status is ACTIVE | DISABLED. Roles are set separately. */
public record UpdateStaffRequest(String status) {
}

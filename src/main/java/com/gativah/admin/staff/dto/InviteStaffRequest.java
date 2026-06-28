package com.gativah.admin.staff.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record InviteStaffRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        @NotEmpty List<Long> roleIds,
        @NotBlank @Size(min = 10) String password) {
}

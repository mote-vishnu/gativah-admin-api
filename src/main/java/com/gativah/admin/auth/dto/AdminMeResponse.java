package com.gativah.admin.auth.dto;

import java.util.List;

/** The signed-in operator's identity + effective authorities. */
public record AdminMeResponse(Long id, String email, String name, String role, List<String> authorities) {
}

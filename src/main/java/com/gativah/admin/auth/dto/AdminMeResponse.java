package com.gativah.admin.auth.dto;

import java.util.List;

/** The signed-in operator's identity, assigned roles, and effective permissions. */
public record AdminMeResponse(Long id, String email, String name, List<String> roles, List<String> permissions) {
}

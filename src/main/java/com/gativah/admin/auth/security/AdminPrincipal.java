package com.gativah.admin.auth.security;

import com.gativah.admin.auth.model.AdminRole;

/** Authenticated staff identity placed in the SecurityContext by the JWT filter. */
public record AdminPrincipal(Long id, String email, String name, AdminRole role) {
}

package com.gativah.admin.auth.security;

import java.util.List;

/**
 * Authenticated staff identity placed in the SecurityContext by the JWT filter.
 * Effective permissions live in the Authentication authorities; {@code roles}
 * are the assigned role names (for display). {@code jti} is the session id from
 * the token (null for legacy tokens), used to re-issue on refresh.
 */
public record AdminPrincipal(Long id, String email, String name, List<String> roles, String jti) {
}

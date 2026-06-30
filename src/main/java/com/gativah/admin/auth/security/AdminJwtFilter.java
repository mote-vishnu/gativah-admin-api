package com.gativah.admin.auth.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.gativah.admin.auth.model.AdminSession;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminSessionRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the {@code Authorization: Bearer} staff token and populates the context.
 * Beyond signature/expiry it re-checks the operator against the DB each request,
 * so a disabled account or bumped token-version (forced logout) is rejected
 * immediately rather than only at token expiry.
 */
@Component
public class AdminJwtFilter extends OncePerRequestFilter {

    private final AdminJwtService jwtService;
    private final AdminUserRepository users;
    private final AdminSessionRepository sessions;

    public AdminJwtFilter(AdminJwtService jwtService, AdminUserRepository users, AdminSessionRepository sessions) {
        this.jwtService = jwtService;
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AdminJwtService.Parsed parsed = jwtService.parse(header.substring(7));
                AdminUser user = users.findById(parsed.principal().id()).orElse(null);
                if (user == null || !user.isActive() || user.getTokenVersion() != parsed.tokenVersion()) {
                    // Account removed/disabled or forced logout (token-version bumped).
                    throw new IllegalStateException("Stale or revoked admin token");
                }
                // Per-session revoke: tokens carry a jti (older tokens may not — those skip this check).
                if (parsed.jti() != null) {
                    AdminSession session = sessions.findByJti(parsed.jti()).orElse(null);
                    if (session == null || session.isRevoked()) {
                        throw new IllegalStateException("Revoked session");
                    }
                }
                List<SimpleGrantedAuthority> authorities = parsed.authorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(parsed.principal(), null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Invalid/expired token → leave context empty; the entry point returns 401.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}

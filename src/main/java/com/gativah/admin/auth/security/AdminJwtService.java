package com.gativah.admin.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import com.gativah.admin.auth.model.AdminUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and parses staff JWTs — a distinct secret + audience from the consumer
 * (pacegrit-service) tokens, so a consumer token can never authenticate to the
 * admin API and vice-versa.
 */
@Service
public class AdminJwtService {

    private final SecretKey key;
    private final String audience;
    private final long expirationMs;

    public AdminJwtService(
            @Value("${admin.jwt.secret}") String secret,
            @Value("${admin.jwt.audience:gativah-admin}") String audience,
            @Value("${admin.jwt.expiration-ms:1800000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(decodeSecret(secret));
        this.audience = audience;
        this.expirationMs = expirationMs;
    }

    public long expirationMs() {
        return expirationMs;
    }

    public String generate(AdminUser user, List<String> authorities) {
        return generate(user, authorities, null);
    }

    public String generate(AdminUser user, List<String> authorities, String jti) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        var builder = Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("roles", user.roleNames())
                .claim("authorities", authorities)
                .claim("tv", user.getTokenVersion())
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(exp);
        if (jti != null) {
            builder = builder.id(jti);
        }
        return builder.signWith(key).compact();
    }

    /** Parsed staff token: principal + Spring authorities + token version + session id (jti). */
    public record Parsed(AdminPrincipal principal, List<String> authorities, int tokenVersion, String jti) {
    }

    public Parsed parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Claims c = jws.getPayload();
        if (c.getAudience() == null || !c.getAudience().contains(audience)) {
            throw new IllegalArgumentException("Wrong token audience");
        }
        Long uid = c.get("uid", Number.class).longValue();
        @SuppressWarnings("unchecked")
        List<String> roles = c.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> authorities = c.get("authorities", List.class);
        Number tv = c.get("tv", Number.class);
        AdminPrincipal principal = new AdminPrincipal(uid, c.getSubject(), c.get("name", String.class),
                roles == null ? List.of() : roles, c.getId());
        return new Parsed(principal, authorities, tv == null ? 0 : tv.intValue(), c.getId());
    }

    /** Dev secret is base64; fall back to raw UTF-8 bytes if it isn't. */
    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // not base64
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }
}

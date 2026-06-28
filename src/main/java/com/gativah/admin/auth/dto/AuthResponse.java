package com.gativah.admin.auth.dto;

/**
 * Login / MFA result. When {@code mfaRequired} is true, {@code token} is null
 * and the client must call verify-mfa with a TOTP code.
 */
public record AuthResponse(String token, Long expiresInMs, boolean mfaRequired, AdminMeResponse user) {

    public static AuthResponse mfaChallenge() {
        return new AuthResponse(null, null, true, null);
    }

    public static AuthResponse issued(String token, long expiresInMs, AdminMeResponse user) {
        return new AuthResponse(token, expiresInMs, false, user);
    }
}

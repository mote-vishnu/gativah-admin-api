package com.gativah.admin.auth.security;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * RFC 6238 TOTP (SHA-1, 30s step, 6 digits) verification for staff MFA.
 * Verifies the current code with a ±1 step window to tolerate clock drift.
 */
@Service
public class TotpService {

    private static final int STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int WINDOW = 1;
    private static final String B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** A fresh base32 TOTP secret (160 bits) for a new enrollment. */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** otpauth:// URI an authenticator app can import (QR or manual). */
    public String provisioningUri(String secret, String account, String issuer) {
        String label = URLEncoder.encode(issuer + ":" + account, StandardCharsets.UTF_8);
        String iss = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + iss
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + STEP_SECONDS;
    }

    public boolean verify(String base32Secret, String code) {
        return verifyAt(base32Secret, code, Instant.now().getEpochSecond());
    }

    /** Testable variant pinned to a fixed epoch-seconds clock. */
    public boolean verifyAt(String base32Secret, String code, long epochSeconds) {
        if (base32Secret == null || code == null || code.isBlank()) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        if (key.length == 0) {
            return false;
        }
        long counter = epochSeconds / STEP_SECONDS;
        String candidate = code.trim();
        for (int w = -WINDOW; w <= WINDOW; w++) {
            if (hotp(key, counter + w).equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String hotp(byte[] key, long counter) {
        try {
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] h = mac.doFinal(data);
            int off = h[h.length - 1] & 0x0f;
            int bin = ((h[off] & 0x7f) << 24)
                    | ((h[off + 1] & 0xff) << 16)
                    | ((h[off + 2] & 0xff) << 8)
                    | (h[off + 3] & 0xff);
            int otp = bin % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP computation failed", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(B32.charAt((buffer >>> (bits - 5)) & 0x1f));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(B32.charAt((buffer << (5 - bits)) & 0x1f));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String s) {
        String clean = s.trim().replace(" ", "").replace("-", "").toUpperCase().replace("=", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bits = 0;
        int value = 0;
        for (char ch : clean.toCharArray()) {
            int idx = B32.indexOf(ch);
            if (idx < 0) {
                continue;
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((value >>> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}

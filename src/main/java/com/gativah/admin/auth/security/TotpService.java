package com.gativah.admin.auth.security;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
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

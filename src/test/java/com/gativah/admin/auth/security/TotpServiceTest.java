package com.gativah.admin.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService totp = new TotpService();

    // RFC 6238 test key "12345678901234567890" (ASCII) encoded as base32.
    private static final String SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void accepts_the_correct_code_for_the_step() {
        // At T=59s (step 30 → counter 1) the SHA-1 TOTP is 287082.
        assertThat(totp.verifyAt(SECRET, "287082", 59)).isTrue();
    }

    @Test
    void rejects_a_wrong_code() {
        assertThat(totp.verifyAt(SECRET, "000000", 59)).isFalse();
    }

    @Test
    void tolerates_one_step_of_drift() {
        // 84s → counter 2; the ±1 window still accepts counter-1's code.
        assertThat(totp.verifyAt(SECRET, "287082", 84)).isTrue();
    }

    @Test
    void rejects_codes_outside_the_window() {
        assertThat(totp.verifyAt(SECRET, "287082", 600)).isFalse();
    }

    @Test
    void null_inputs_are_safe() {
        assertThat(totp.verifyAt(null, "287082", 59)).isFalse();
        assertThat(totp.verifyAt(SECRET, null, 59)).isFalse();
    }
}

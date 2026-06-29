package com.gativah.admin.moderation.dto;

import java.math.BigDecimal;

/**
 * One advisory auto-flag signal for a report (e.g. toxicity 0.78). Read-only
 * hint for the moderator — never an automated decision. {@code isBoolean}
 * signals carry {@code boolValue} instead of a {@code score} meter.
 */
public record AutoFlagSignal(
        String key,
        String label,
        BigDecimal score,
        boolean isBoolean,
        Boolean boolValue,
        String severity) {
}

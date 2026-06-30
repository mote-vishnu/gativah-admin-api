package com.gativah.admin.finance.dto;

import java.math.BigDecimal;

/**
 * Estimated payout for a single store/platform over the reporting window.
 * {@code netGross = gross - refunds}; {@code commission = netGross * commissionRate};
 * {@code payout = netGross - commission}. Commission is an ESTIMATE — actual store
 * fees vary by program (small-business 15%, post-year-1 15%, regional taxes).
 */
public record PayoutRow(
        String platform,
        BigDecimal gross,
        BigDecimal refunds,
        BigDecimal netGross,
        long txnCount,
        double commissionRate,
        BigDecimal commission,
        BigDecimal payout) {
}

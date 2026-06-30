package com.gativah.admin.finance.dto;

import java.math.BigDecimal;

/** Headline finance KPIs. Amounts are store-gross (commission not deducted). */
public record FinanceOverview(
        long activeSubscribers,
        long trialing,
        long inGrace,
        long canceled30d,
        long newSubs30d,
        double churnRate,
        Double grossTrendPct,
        BigDecimal mrr,
        BigDecimal arr,
        BigDecimal grossMtd,
        BigDecimal refundsMtd,
        BigDecimal netMtd) {
}

package com.gativah.admin.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/** Per-platform estimated payouts plus totals for the trailing window. */
public record PayoutsResponse(
        int windowDays,
        List<PayoutRow> platforms,
        BigDecimal grossTotal,
        BigDecimal refundTotal,
        BigDecimal netGrossTotal,
        BigDecimal commissionTotal,
        BigDecimal payoutTotal) {
}

package com.gativah.admin.finance.dto;

import java.math.BigDecimal;

/**
 * Approximate MRR movement over the trailing 30 days: start → +new → −churned →
 * end. Estimated from subscription start/cancel dates × normalized plan price
 * (no plan-change history, so expansion/contraction/reactivation aren't split out).
 */
public record MrrMovement(BigDecimal start, BigDecimal added, BigDecimal churned, BigDecimal end) {
}

package com.gativah.admin.finance.dto;

import java.math.BigDecimal;

/** One bucket on the revenue time series. */
public record RevenuePoint(String period, BigDecimal gross, BigDecimal refunds) {
}

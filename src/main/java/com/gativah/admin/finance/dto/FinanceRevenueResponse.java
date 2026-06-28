package com.gativah.admin.finance.dto;

import java.util.List;

public record FinanceRevenueResponse(
        String granularity,
        String groupBy,
        List<RevenuePoint> series,
        List<RevenueSlice> breakdown) {
}

package com.gativah.admin.finance.service;

import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.FinanceRevenueResponse;
import com.gativah.admin.finance.dto.RevenuePoint;
import com.gativah.admin.finance.dto.RevenueSlice;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.WebhookHealth;
import com.gativah.admin.finance.query.FinanceQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FinanceServiceImpl implements FinanceService {

    private static final int DEFAULT_MONTHS = 12;

    private final FinanceQuery query;

    public FinanceServiceImpl(FinanceQuery query) {
        this.query = query;
    }

    @Override
    public FinanceOverview overview() {
        return query.overview();
    }

    @Override
    public FinanceRevenueResponse revenue(String granularity, LocalDateTime from, LocalDateTime to, String groupBy) {
        String unit = "day".equalsIgnoreCase(granularity) ? "day" : "month";
        LocalDateTime end = to != null ? to : LocalDateTime.now();
        LocalDateTime start = from != null ? from : end.minusMonths(DEFAULT_MONTHS);

        List<RevenuePoint> series = query.revenueSeries(unit, start, end);
        List<RevenueSlice> breakdown = groupBy == null || groupBy.isBlank()
                ? List.of()
                : query.revenueBreakdown(groupBy, start, end);
        return new FinanceRevenueResponse(unit, groupBy, series, breakdown);
    }

    @Override
    public Page<TransactionRow> transactions(String platform, String type, String status, String country,
                                             Long userId, Pageable pageable) {
        return query.transactions(platform, type, status, country, userId, pageable);
    }

    @Override
    public Page<SubscriptionRow> subscriptions(String state, Pageable pageable) {
        return query.subscriptions(state, pageable);
    }

    @Override
    public WebhookHealth webhooks() {
        return query.webhookHealth();
    }
}

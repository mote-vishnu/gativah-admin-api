package com.gativah.admin.finance.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.FinanceRevenueResponse;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.WebhookHealth;
import com.gativah.admin.finance.service.FinanceService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Revenue, subscriptions, ledger, and webhook-health dashboards (FINANCE:VIEW). */
@RestController
@PreAuthorize("hasAuthority('FINANCE:VIEW')")
public class AdminFinanceController {

    private final FinanceService service;

    public AdminFinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/finance/overview")
    public FinanceOverview overview() {
        return service.overview();
    }

    @GetMapping("/api/v1/admin/finance/revenue")
    public FinanceRevenueResponse revenue(
            @RequestParam(defaultValue = "month") String granularity,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.revenue(granularity,
                from == null ? null : from.atStartOfDay(),
                // inclusive end: cover the whole `to` day (range is half-open `< end`)
                to == null ? null : to.plusDays(1).atStartOfDay(),
                groupBy);
    }

    @GetMapping("/api/v1/admin/finance/transactions")
    public Page<TransactionRow> transactions(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.transactions(platform, type, status, country, userId, pageable);
    }

    @GetMapping("/api/v1/admin/finance/subscriptions")
    public Page<SubscriptionRow> subscriptions(
            @RequestParam(required = false) String state,
            @PageableDefault(size = 20) Pageable pageable) {
        return service.subscriptions(state, pageable);
    }

    @GetMapping("/api/v1/admin/finance/webhooks")
    public WebhookHealth webhooks() {
        return service.webhooks();
    }
}

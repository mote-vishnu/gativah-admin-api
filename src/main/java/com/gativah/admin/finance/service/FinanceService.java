package com.gativah.admin.finance.service;

import java.time.LocalDateTime;

import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.FinanceRevenueResponse;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.WebhookHealth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FinanceService {

    FinanceOverview overview();

    FinanceRevenueResponse revenue(String granularity, LocalDateTime from, LocalDateTime to, String groupBy);

    Page<TransactionRow> transactions(String platform, String type, String status, String country,
                                      Long userId, Pageable pageable);

    Page<SubscriptionRow> subscriptions(String state, Pageable pageable);

    WebhookHealth webhooks();
}

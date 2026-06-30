package com.gativah.admin.finance.service;

import java.time.LocalDateTime;

import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.FinanceRevenueResponse;
import com.gativah.admin.finance.dto.MrrMovement;
import com.gativah.admin.finance.dto.PayoutsResponse;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionDetail;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.WebhookHealth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FinanceService {

    FinanceOverview overview();

    MrrMovement mrrMovement();

    PayoutsResponse payouts(int windowDays);

    FinanceRevenueResponse revenue(String granularity, LocalDateTime from, LocalDateTime to, String groupBy);

    Page<TransactionRow> transactions(String platform, String type, String status, String country,
                                      Long userId, Pageable pageable);

    TransactionDetail transactionDetail(long id);

    Page<SubscriptionRow> subscriptions(String state, Pageable pageable);

    WebhookHealth webhooks();
}

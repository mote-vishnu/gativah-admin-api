package com.gativah.admin.finance.query;

import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.finance.dto.FinanceOverview;
import com.gativah.admin.finance.dto.MrrMovement;
import com.gativah.admin.finance.dto.PayoutsResponse;
import com.gativah.admin.finance.dto.RevenuePoint;
import com.gativah.admin.finance.dto.RevenueSlice;
import com.gativah.admin.finance.dto.SubscriptionRow;
import com.gativah.admin.finance.dto.TransactionDetail;
import com.gativah.admin.finance.dto.TransactionRow;
import com.gativah.admin.finance.dto.WebhookHealth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read side for the finance dashboards — aggregation over the billing tables. */
public interface FinanceQuery {

    FinanceOverview overview();

    MrrMovement mrrMovement();

    PayoutsResponse payouts(int windowDays);

    List<RevenuePoint> revenueSeries(String granularity, LocalDateTime from, LocalDateTime to);

    List<RevenueSlice> revenueBreakdown(String groupBy, LocalDateTime from, LocalDateTime to);

    Page<TransactionRow> transactions(String platform, String type, String status, String country,
                                      Long userId, Pageable pageable);

    TransactionDetail transactionDetail(long id);

    Page<SubscriptionRow> subscriptions(String state, Pageable pageable);

    WebhookHealth webhookHealth();
}

package com.gativah.admin.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full drill-down for a single billing transaction: every stored column, plus the
 * owning subscription (if any), the rest of its store-transaction chain, and the
 * webhook events that touched it.
 */
public record TransactionDetail(
        long id,
        Long userId,
        Long subscriptionId,
        String planCode,
        String platform,
        String productId,
        String storeTransactionId,
        String originalTransactionId,
        String type,
        String status,
        BigDecimal grossAmount,
        String grossCurrency,
        String countryCode,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        LocalDateTime purchasedAt,
        String environment,
        String source,
        String notificationUuid,
        LocalDateTime createdAt,
        SubscriptionRow subscription,
        List<TransactionRow> relatedTxns,
        List<TxnEventRow> events) {
}

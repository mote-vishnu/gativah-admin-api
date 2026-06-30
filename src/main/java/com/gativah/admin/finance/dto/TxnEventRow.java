package com.gativah.admin.finance.dto;

import java.time.LocalDateTime;

/** A store webhook event matched to a transaction (by original-txn / notification id). */
public record TxnEventRow(
        long id,
        String platform,
        String eventType,
        String subtype,
        String status,
        LocalDateTime receivedAt,
        LocalDateTime processedAt) {
}

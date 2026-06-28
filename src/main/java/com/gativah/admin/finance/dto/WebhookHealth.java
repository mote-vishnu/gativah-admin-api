package com.gativah.admin.finance.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Store-webhook (billing_event) health, incl. the dead-letter backlog. */
public record WebhookHealth(
        long received24h,
        long processed24h,
        long failed24h,
        long deadLetter,
        LocalDateTime lastProcessedAt,
        List<DeadLetterRow> recentDeadLetters) {
}

package com.gativah.admin.users.dto;

import java.math.BigDecimal;
import java.util.List;

/** Billing summary + transaction history for the profile Billing tab. */
public record UserBilling(
        BigDecimal lifetimeValue,
        String currency,
        long refunds,
        long transactions,
        List<UserTxnRow> items) {
}

package com.gativah.admin.legal.dto;

/** Compliance KPI band for the Legal & Disclosure overview. */
public record LegalStats(
        long openRequests,
        long underReview,
        long pendingApproval,
        long overdue,
        long actioned30d,
        long disclosures30d,
        long openTasks) {
}

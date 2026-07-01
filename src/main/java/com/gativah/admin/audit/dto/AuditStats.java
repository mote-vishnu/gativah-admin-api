package com.gativah.admin.audit.dto;

/** Summary counts for the audit log KPI band (respecting the viewer's scope). */
public record AuditStats(long total, long today, long last7d, long operators) {
}

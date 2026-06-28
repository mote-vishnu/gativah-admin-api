package com.gativah.admin.audit.service;

import com.gativah.admin.audit.model.AdminAuditLog;
import com.gativah.admin.audit.repo.AdminAuditLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the immutable operator audit trail. Every mutating admin action must
 * call this (Phase 1: explicit calls; an interceptor can enforce it later).
 */
@Service
public class AuditService {

    private final AdminAuditLogRepository repo;

    public AuditService(AdminAuditLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(Long adminUserId, String action, String targetType, String targetId,
                       String summary, String metadata, String ip) {
        AdminAuditLog row = new AdminAuditLog();
        row.setAdminUserId(adminUserId);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setSummary(summary);
        row.setMetadata(metadata);
        row.setIp(ip);
        repo.save(row);
    }

    @Transactional
    public void record(Long adminUserId, String action, String summary) {
        record(adminUserId, action, null, null, summary, null, null);
    }
}

package com.gativah.admin.audit.service;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;

import com.gativah.admin.audit.dto.AuditEntryRow;
import com.gativah.admin.audit.model.AdminAuditLog;
import com.gativah.admin.audit.repo.AdminAuditLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes the immutable operator audit trail. Every mutating admin action must
 * call this (Phase 1: explicit calls; an interceptor can enforce it later).
 * The caller's IP is captured automatically from the in-flight request.
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
        row.setIp(ip != null ? ip : currentIp());
        repo.save(row);
    }

    @Transactional
    public void record(Long adminUserId, String action, String summary) {
        record(adminUserId, action, null, null, summary, null, null);
    }

    /** Filtered audit feed. {@code actorId} scopes to one operator; {@code targetType}/{@code targetId} scope to one entity (all null = everything). */
    @Transactional(readOnly = true)
    public Page<AuditEntryRow> list(Long actorId, String action, String targetType, String targetId,
                                    LocalDateTime from, LocalDateTime to, String q, Pageable pageable) {
        return repo.search(actorId, blankToNull(action), blankToNull(targetType), blankToNull(targetId),
                        from, to, blankToNull(q), pageable)
                .map(a -> new AuditEntryRow(a.getId(), a.getAdminUserId(), a.getAction(),
                        a.getTargetType(), a.getTargetId(), a.getSummary(), a.getIp(), a.getCreatedAt()));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Best-effort client IP from the in-flight request (X-Forwarded-For aware). */
    private static String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (RuntimeException e) {
            return null;
        }
    }
}

package com.gativah.admin.audit.repo;

import java.time.LocalDateTime;

import com.gativah.admin.audit.model.AdminAuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /**
     * Filtered audit feed — every filter is nullable (skipped when null). Pass a
     * non-null actorId to scope to one operator (also used to force self-scope for
     * operators without AUDIT:VIEW). {@code action} is a prefix match (e.g. "USER_").
     */
    @Query("""
            select a from AdminAuditLog a
            where (:actorId is null or a.adminUserId = :actorId)
              and (:action is null or a.action like concat(:action, '%'))
              and (:category is null or
                   (case
                      when a.action like 'REPORT_%' or a.action like 'CONTENT_%' or a.action like 'USER_%'
                           or a.action like 'APPEAL_%' or a.action like 'CLUB_%' or a.action like 'REGION_%' then 'MODERATION'
                      when a.action like 'ENTITLEMENT_%' or a.action like 'FINANCE_%' or a.action like 'BILLING_%' then 'FINANCE'
                      when a.action like 'STAFF_%' or a.action like 'ROLE_%' then 'STAFF'
                      when a.action like 'LOGIN%' or a.action like 'MFA%' or a.action like 'AUTH_%' then 'AUTH'
                      when a.action like 'LEGAL_%' then 'LEGAL'
                      else 'OTHER' end) = :category)
              and (:targetType is null or a.targetType = :targetType)
              and (:targetId is null or a.targetId = :targetId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt < :to)
              and (:q is null or lower(a.summary) like lower(concat('%', :q, '%'))
                   or lower(a.action) like lower(concat('%', :q, '%')))
            order by a.createdAt desc
            """)
    Page<AdminAuditLog> search(@Param("actorId") Long actorId,
                               @Param("action") String action,
                               @Param("category") String category,
                               @Param("targetType") String targetType,
                               @Param("targetId") String targetId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("q") String q,
                               Pageable pageable);

    @Query("select count(a) from AdminAuditLog a where (:actorId is null or a.adminUserId = :actorId) "
            + "and (:since is null or a.createdAt >= :since)")
    long countScoped(@Param("actorId") Long actorId, @Param("since") LocalDateTime since);

    @Query("select count(distinct a.adminUserId) from AdminAuditLog a")
    long countOperators();
}

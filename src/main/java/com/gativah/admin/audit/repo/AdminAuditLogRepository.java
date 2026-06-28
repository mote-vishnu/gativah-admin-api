package com.gativah.admin.audit.repo;

import com.gativah.admin.audit.model.AdminAuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(Long adminUserId, Pageable pageable);

    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

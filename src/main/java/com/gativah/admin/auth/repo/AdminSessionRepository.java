package com.gativah.admin.auth.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.gativah.admin.auth.model.AdminSession;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {

    Optional<AdminSession> findByJti(String jti);

    List<AdminSession> findByAdminUserIdOrderByCreatedAtDesc(Long adminUserId);

    List<AdminSession> findByRevokedFalseOrderByCreatedAtDesc();

    long countByRevokedFalse();

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);
}

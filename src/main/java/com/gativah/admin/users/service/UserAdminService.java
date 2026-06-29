package com.gativah.admin.users.service;

import java.util.List;

import com.gativah.admin.audit.dto.AuditEntryRow;
import com.gativah.admin.users.dto.BanRequest;
import com.gativah.admin.users.dto.SuspendRequest;
import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.dto.UserInsights;
import com.gativah.admin.users.dto.UserSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    Page<UserSummary> list(String q, List<String> statuses, Pageable pageable);

    UserDetail detail(Long id);

    /** Admin actions taken against this user (audit entries where target = USER:id). */
    Page<AuditEntryRow> audit(Long userId, Pageable pageable);

    UserDetail suspend(Long actorAdminId, Long userId, SuspendRequest req);

    UserDetail ban(Long actorAdminId, Long userId, BanRequest req);

    UserDetail reinstate(Long actorAdminId, Long userId);

    UserDetail setVerified(Long actorAdminId, Long userId, boolean grant);

    UserInsights insights(Long userId);
}

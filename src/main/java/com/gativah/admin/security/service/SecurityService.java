package com.gativah.admin.security.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gativah.admin.auth.model.AdminSession;
import com.gativah.admin.auth.model.AdminUser;
import com.gativah.admin.auth.repo.AdminSessionRepository;
import com.gativah.admin.auth.repo.AdminUserRepository;
import com.gativah.admin.security.dto.ActiveSessionRow;
import com.gativah.admin.security.dto.SecurityOverview;
import com.gativah.admin.security.dto.UnenrolledAdmin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side for the org-wide admin security-posture surface. */
@Service
@Transactional(readOnly = true)
public class SecurityService {

    private final AdminUserRepository users;
    private final AdminSessionRepository sessions;

    public SecurityService(AdminUserRepository users, AdminSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public SecurityOverview overview() {
        List<UnenrolledAdmin> unenrolled = users
                .findByMfaEnrolledFalseAndStatusOrderByNameAsc(AdminUser.STATUS_ACTIVE).stream()
                .map(u -> new UnenrolledAdmin(u.getId(), u.getName(), u.getEmail()))
                .toList();
        return new SecurityOverview(
                users.countByMfaEnrolledTrue(),
                users.count(),
                users.countByStatus(AdminUser.STATUS_ACTIVE),
                sessions.countByRevokedFalse(),
                sessions.countByCreatedAtGreaterThanEqual(LocalDateTime.now().minusDays(7)),
                unenrolled);
    }

    public List<ActiveSessionRow> activeSessions() {
        List<AdminSession> active = sessions.findByRevokedFalseOrderByCreatedAtDesc();
        List<Long> ids = active.stream().map(AdminSession::getAdminUserId).distinct().toList();
        Map<Long, AdminUser> byId = users.findAllById(ids).stream()
                .collect(Collectors.toMap(AdminUser::getId, Function.identity()));
        return active.stream().map(s -> {
            AdminUser u = byId.get(s.getAdminUserId());
            return new ActiveSessionRow(s.getId(), s.getAdminUserId(),
                    u != null ? u.getName() : null, u != null ? u.getEmail() : null,
                    s.getIp(), s.getUserAgent(), s.getCreatedAt());
        }).toList();
    }
}

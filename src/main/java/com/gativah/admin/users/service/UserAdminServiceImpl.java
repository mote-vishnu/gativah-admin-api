package com.gativah.admin.users.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.gativah.admin.audit.dto.AuditEntryRow;
import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.users.dto.BanRequest;
import com.gativah.admin.users.dto.SuspendRequest;
import com.gativah.admin.users.dto.UserBilling;
import com.gativah.admin.users.dto.UserContentResponse;
import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.dto.UserInsights;
import com.gativah.admin.users.dto.UserReportsResponse;
import com.gativah.admin.users.dto.UserSummary;
import com.gativah.admin.users.dto.UserTxnRow;
import com.gativah.admin.users.query.UsersQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private static final int DEFAULT_SUSPEND_DAYS = 7;

    private final UsersQuery query;
    private final PacegritInternalClient internal;
    private final AuditService audit;

    public UserAdminServiceImpl(UsersQuery query, PacegritInternalClient internal, AuditService audit) {
        this.query = query;
        this.internal = internal;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummary> list(String q, List<String> statuses, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim() + "%";
        return query.search(like, statuses, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetail detail(Long id) {
        UserDetail detail = query.detail(id);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        return detail;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEntryRow> audit(Long userId, Pageable pageable) {
        return audit.list(null, null, "USER", String.valueOf(userId), null, null, null, pageable);
    }

    @Override
    public UserDetail suspend(Long actorAdminId, Long userId, SuspendRequest req) {
        requireUser(userId);
        int days = req.days() != null && req.days() > 0 ? req.days() : DEFAULT_SUSPEND_DAYS;
        internal.suspendUser(actorAdminId, userId, req.reason(), LocalDateTime.now().plusDays(days));
        audit.record(actorAdminId, "USER_SUSPEND", "USER", String.valueOf(userId),
                days + "d — " + req.reason(), null, null);
        return detail(userId);
    }

    @Override
    public UserDetail ban(Long actorAdminId, Long userId, BanRequest req) {
        requireUser(userId);
        internal.banUser(actorAdminId, userId, req.reason());
        audit.record(actorAdminId, "USER_BAN", "USER", String.valueOf(userId), req.reason(), null, null);
        return detail(userId);
    }

    @Override
    public UserDetail reinstate(Long actorAdminId, Long userId) {
        requireUser(userId);
        internal.reinstateUser(actorAdminId, userId);
        audit.record(actorAdminId, "USER_REINSTATE", "USER", String.valueOf(userId), "reinstated", null, null);
        return detail(userId);
    }

    @Override
    public UserDetail setVerified(Long actorAdminId, Long userId, boolean grant) {
        requireUser(userId);
        internal.setVerified(actorAdminId, userId, grant);
        audit.record(actorAdminId, grant ? "USER_VERIFY" : "USER_UNVERIFY", "USER", String.valueOf(userId),
                grant ? "verified badge granted" : "verified badge revoked", null, null);
        return detail(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInsights insights(Long userId) {
        String status = query.accountStatus(userId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
        long reports = query.reportsAgainst(userId);
        long sanctions = query.sanctionCount(userId);
        int score = riskScore(reports, sanctions, status);
        String level = score >= 60 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
        return new UserInsights(reports, sanctions,
                query.followerCount(userId), query.followingCount(userId), query.postCount(userId),
                score, level, query.devices(userId), query.activity(userId, 90));
    }

    @Override
    @Transactional(readOnly = true)
    public UserContentResponse content(Long userId) {
        return new UserContentResponse(query.content(userId, 40));
    }

    @Override
    @Transactional(readOnly = true)
    public UserBilling billing(Long userId) {
        List<UserTxnRow> txns = query.transactions(userId, 50);
        BigDecimal ltv = BigDecimal.ZERO;
        long refunds = 0;
        String currency = null;
        for (UserTxnRow t : txns) {
            boolean refund = t.type() != null && t.type().toUpperCase().contains("REFUND");
            if (refund) {
                refunds++;
            } else if (t.amount() != null) {
                ltv = ltv.add(t.amount());
            }
            if (currency == null && t.currency() != null) {
                currency = t.currency();
            }
        }
        return new UserBilling(ltv, currency == null ? "USD" : currency, refunds, txns.size(), txns);
    }

    @Override
    @Transactional(readOnly = true)
    public UserReportsResponse reportsAgainst(Long userId) {
        return new UserReportsResponse(query.reportsAgainstList(userId, 40));
    }

    private int riskScore(long reports, long sanctions, String status) {
        long base = reports * 8 + sanctions * 15;
        if ("BANNED".equals(status)) {
            base += 40;
        } else if ("SUSPENDED".equals(status)) {
            base += 20;
        }
        return (int) Math.min(100, base);
    }

    private void requireUser(Long userId) {
        if (query.detail(userId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
    }
}

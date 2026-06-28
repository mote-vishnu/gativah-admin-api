package com.gativah.admin.moderation.service;

import java.time.LocalDateTime;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.moderation.dto.AppealResolveRequest;
import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.ModerationActionRow;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportSummary;
import com.gativah.admin.moderation.dto.ResolveRequest;
import com.gativah.admin.moderation.dto.ResolveResponse;
import com.gativah.admin.moderation.model.Appeal;
import com.gativah.admin.moderation.model.ContentReport;
import com.gativah.admin.moderation.model.ModerationAction;
import com.gativah.admin.moderation.query.ModerationQuery;
import com.gativah.admin.moderation.repo.AppealRepository;
import com.gativah.admin.moderation.repo.ContentReportRepository;
import com.gativah.admin.moderation.repo.ModerationActionRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationServiceImpl implements ModerationService {

    private static final int DEFAULT_SUSPEND_DAYS = 7;
    private static final String TARGET_USER = "USER";

    private final ModerationQuery query;
    private final ContentReportRepository reports;
    private final ModerationActionRepository actions;
    private final AppealRepository appeals;
    private final PacegritInternalClient internal;
    private final AuditService audit;

    public ModerationServiceImpl(ModerationQuery query, ContentReportRepository reports,
                                 ModerationActionRepository actions, AppealRepository appeals,
                                 PacegritInternalClient internal, AuditService audit) {
        this.query = query;
        this.reports = reports;
        this.actions = actions;
        this.appeals = appeals;
        this.internal = internal;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportSummary> queue(String status, String contentType, String reason, Pageable pageable) {
        return query.queue(status, contentType, reason, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportDetail detail(Long reportId) {
        ReportDetail detail = query.detail(reportId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found: " + reportId);
        }
        return detail;
    }

    @Override
    @Transactional
    public ResolveResponse resolve(Long actorAdminId, Long reportId, ResolveRequest req) {
        ContentReport report = reports.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found: " + reportId));

        String targetType = report.getContentType();
        Long targetId = report.getContentId();
        String newStatus = ContentReport.STATUS_RESOLVED;

        switch (req.action()) {
            case DISMISS -> newStatus = ContentReport.STATUS_DISMISSED;
            case WARN -> { /* recorded only — no content/account change */ }
            case TAKEDOWN -> internal.takedown(actorAdminId, report.getContentType(), report.getContentId(), req.reason());
            case SUSPEND -> {
                Long author = authorOrThrow(report);
                int days = req.suspendDays() != null ? req.suspendDays() : DEFAULT_SUSPEND_DAYS;
                internal.suspendUser(actorAdminId, author, req.reason(), LocalDateTime.now().plusDays(days));
                targetType = TARGET_USER;
                targetId = author;
            }
            case BAN -> {
                Long author = authorOrThrow(report);
                internal.banUser(actorAdminId, author, req.reason());
                targetType = TARGET_USER;
                targetId = author;
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported action");
        }

        report.setStatus(newStatus);
        report.setReviewedBy(actorAdminId);
        report.setReviewedAt(LocalDateTime.now());
        reports.save(report);

        ModerationAction action = new ModerationAction();
        action.setReportId(reportId);
        action.setAdminUserId(actorAdminId);
        action.setTargetType(targetType);
        action.setTargetId(targetId);
        action.setAction(req.action().name());
        action.setReason(req.reason());
        actions.save(action);

        audit.record(actorAdminId, "REPORT_RESOLVE", "REPORT", String.valueOf(reportId),
                req.action() + " on " + targetType + " " + targetId, null, null);

        return new ResolveResponse(true, newStatus, "Report resolved");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationActionRow> history(Pageable pageable) {
        return actions.findAllByOrderByCreatedAtDesc(pageable).map(a -> new ModerationActionRow(
                a.getId(), a.getReportId(), a.getAdminUserId(), a.getTargetType(),
                a.getTargetId(), a.getAction(), a.getReason(), a.getCreatedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppealRow> appeals(String status, Pageable pageable) {
        Page<Appeal> page = status == null
                ? appeals.findAllByOrderByCreatedAtDesc(pageable)
                : appeals.findByStatusOrderByCreatedAtDesc(status, pageable);
        return page.map(a -> new AppealRow(a.getId(), a.getSubjectUserId(), a.getRelatedReportId(),
                a.getRelatedActionId(), a.getMessage(), a.getStatus(), a.getCreatedAt()));
    }

    @Override
    @Transactional
    public void resolveAppeal(Long actorAdminId, Long appealId, AppealResolveRequest req) {
        Appeal appeal = appeals.findById(appealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appeal not found: " + appealId));
        appeal.setStatus(req.grant() ? Appeal.STATUS_GRANTED : Appeal.STATUS_DENIED);
        appeal.setResolvedBy(actorAdminId);
        appeal.setResolvedAt(LocalDateTime.now());
        appeals.save(appeal);

        if (req.grant()) {
            // Granting an appeal lifts the sanction.
            internal.reinstateUser(actorAdminId, appeal.getSubjectUserId());
        }
        audit.record(actorAdminId, "APPEAL_RESOLVE", "APPEAL", String.valueOf(appealId),
                (req.grant() ? "GRANTED" : "DENIED") + " — " + req.note(), null, null);
    }

    private Long authorOrThrow(ContentReport report) {
        Long author = query.authorOf(report.getContentType(), report.getContentId());
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot resolve content author for report " + report.getId());
        }
        return author;
    }
}

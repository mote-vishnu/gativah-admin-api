package com.gativah.admin.moderation.service;

import java.util.List;

import com.gativah.admin.moderation.dto.AppealResolveRequest;
import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.ModerationActionRow;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportSummary;
import com.gativah.admin.moderation.dto.ResolveRequest;
import com.gativah.admin.moderation.dto.ResolveResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ModerationService {

    Page<ReportSummary> queue(String status, String contentType, String reason, Pageable pageable);

    ReportDetail detail(Long reportId);

    ResolveResponse resolve(Long actorAdminId, Long reportId, ResolveRequest req);

    Page<ModerationActionRow> history(Pageable pageable);

    Page<AppealRow> appeals(String status, Pageable pageable);

    void resolveAppeal(Long actorAdminId, Long appealId, AppealResolveRequest req);

    void assign(Long actorAdminId, Long reportId, Long assigneeAdminId);

    void bulkAssign(Long actorAdminId, List<Long> ids, Long assigneeAdminId);
}

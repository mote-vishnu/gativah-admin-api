package com.gativah.admin.moderation.query;

import java.util.List;

import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.ReportSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read side of the grievance queue — cross-table joins kept out of JPA. */
public interface ModerationQuery {

    Page<ReportSummary> queue(List<String> statuses, String contentType, String reason, Pageable pageable);

    ReportDetail detail(Long reportId);

    /** Author user-id of the reported content, or null if it can't be resolved. */
    Long authorOf(String contentType, Long contentId);
}

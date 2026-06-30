package com.gativah.admin.moderation.query;

import java.util.List;

import com.gativah.admin.moderation.dto.AppealRow;
import com.gativah.admin.moderation.dto.AuthorSanction;
import com.gativah.admin.moderation.dto.AuthorStats;
import com.gativah.admin.moderation.dto.AutoFlagSignal;
import com.gativah.admin.moderation.dto.ReasonCount;
import com.gativah.admin.moderation.dto.ReportDetail;
import com.gativah.admin.moderation.dto.RegionBanRow;
import com.gativah.admin.moderation.dto.ReportStats;
import com.gativah.admin.moderation.dto.ReportSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read side of the grievance queue — cross-table joins kept out of JPA. */
public interface ModerationQuery {

    Page<ReportSummary> queue(List<String> statuses, String contentType, String reason, Pageable pageable);

    ReportDetail detail(Long reportId);

    /** Author user-id of the reported content, or null if it can't be resolved. */
    Long authorOf(String contentType, Long contentId);

    /** Open-queue (PENDING/REVIEWING) report counts grouped by reason, busiest first. */
    List<ReasonCount> queueByReason();

    /** Queue health KPIs (open / SLA breaches / resolved-24h / repeat offenders). */
    ReportStats stats();

    /** Region-bans (geo-restrictions on posts), active first. */
    List<RegionBanRow> regionBans();

    /** Appeals enriched with the subject's username + the original action. */
    Page<AppealRow> appeals(String status, Pageable pageable);

    /** How many reports target content authored by this user. */
    long reportsAgainst(Long authorUserId);

    /** This user's most recent sanctions (newest first). */
    List<AuthorSanction> recentSanctions(Long authorUserId, int limit);

    /** Aggregated standing/reach for an author (status, followers, plan, member-since, report counts). */
    AuthorStats authorStats(Long authorUserId);

    /** Advisory auto-flag signals attached to a report (severity-ordered). */
    List<AutoFlagSignal> signals(Long reportId);
}

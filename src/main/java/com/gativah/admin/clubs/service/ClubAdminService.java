package com.gativah.admin.clubs.service;

import java.util.List;

import com.gativah.admin.audit.dto.AuditEntryRow;
import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubEventDetail;
import com.gativah.admin.clubs.dto.ClubMemberRow;
import com.gativah.admin.clubs.dto.ClubReportedContent;
import com.gativah.admin.clubs.dto.ClubStats;
import com.gativah.admin.clubs.dto.ClubSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubAdminService {

    Page<ClubSummary> list(String q, List<String> visibilities, List<String> statuses, Pageable pageable);

    ClubStats stats();

    ClubDetail detail(Long id);

    Page<ClubMemberRow> members(Long id, String role, String status, String q, Pageable pageable);

    Page<AuditEntryRow> audit(Long id, Pageable pageable);

    ClubDetail removeClub(Long actorAdminId, Long id, String reason);

    ClubDetail restoreClub(Long actorAdminId, Long id);

    ClubDetail removeMember(Long actorAdminId, Long id, Long userId);

    ClubDetail removeEvent(Long actorAdminId, Long id, Long eventId, String reason);

    ClubEventDetail eventDetail(Long clubId, Long eventId);

    ClubEventDetail restoreEvent(Long actorAdminId, Long clubId, Long eventId);

    List<ClubReportedContent> reportedContent(Long clubId);
}

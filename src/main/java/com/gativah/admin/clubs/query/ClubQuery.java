package com.gativah.admin.clubs.query;

import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubEventDetail;
import com.gativah.admin.clubs.dto.ClubMemberRow;
import com.gativah.admin.clubs.dto.ClubReportedContent;
import com.gativah.admin.clubs.dto.ClubStats;
import com.gativah.admin.clubs.dto.ClubSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubQuery {

    Page<ClubSummary> search(String q, List<String> visibilities, List<Boolean> removed, Pageable pageable);

    /** Club + members + events, or null if it doesn't exist. */
    ClubDetail detail(Long id);

    /** Directory-wide KPI band. */
    ClubStats stats();

    /** Paged, filtered members of one club (role/status/username). */
    Page<ClubMemberRow> members(Long clubId, String role, String status, String q, Pageable pageable);

    /** One club event with RSVP tallies/list + route points, or null if it doesn't belong to the club. */
    ClubEventDetail eventDetail(Long clubId, Long eventId);

    /** Reported posts/comments inside the club (grouped by content, newest report first). */
    List<ClubReportedContent> reportedContent(Long clubId);
}

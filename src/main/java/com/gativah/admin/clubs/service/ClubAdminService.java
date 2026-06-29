package com.gativah.admin.clubs.service;

import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubAdminService {

    Page<ClubSummary> list(String q, List<String> visibilities, List<String> statuses, Pageable pageable);

    ClubDetail detail(Long id);

    ClubDetail removeClub(Long actorAdminId, Long id, String reason);

    ClubDetail restoreClub(Long actorAdminId, Long id);

    ClubDetail removeMember(Long actorAdminId, Long id, Long userId);

    ClubDetail removeEvent(Long actorAdminId, Long id, Long eventId, String reason);
}

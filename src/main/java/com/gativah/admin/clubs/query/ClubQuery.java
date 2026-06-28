package com.gativah.admin.clubs.query;

import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClubQuery {

    Page<ClubSummary> search(String q, List<String> visibilities, List<Boolean> removed, Pageable pageable);

    /** Club + members + events, or null if it doesn't exist. */
    ClubDetail detail(Long id);
}

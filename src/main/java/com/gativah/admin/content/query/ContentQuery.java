package com.gativah.admin.content.query;

import java.util.List;

import com.gativah.admin.content.dto.ContentDetail;
import com.gativah.admin.content.dto.ContentReportRef;
import com.gativah.admin.content.dto.ContentRow;
import com.gativah.admin.content.dto.ContentStats;
import com.gativah.admin.content.dto.StoryRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentQuery {

    /** types: POST/COMMENT (null = all); removed: removed-flags (null = all); reportedOnly: only content with open reports. */
    Page<ContentRow> search(List<String> types, String q, List<Boolean> removed, boolean reportedOnly, Pageable pageable);

    Page<StoryRow> stories(String q, Pageable pageable);

    ContentStats stats();

    /** Moderation reports filed against one piece of content (all statuses, newest first). */
    List<ContentReportRef> reportsFor(String type, Long id);

    /** Full post/comment drill-down (body + media + reactions + comments), or null if missing. */
    ContentDetail contentDetail(String type, Long id);
}

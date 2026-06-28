package com.gativah.admin.content.query;

import java.util.List;

import com.gativah.admin.content.dto.ContentRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentQuery {

    /** types: POST/COMMENT (null = all); removed: removed-flags (null = all). */
    Page<ContentRow> search(List<String> types, String q, List<Boolean> removed, Pageable pageable);
}

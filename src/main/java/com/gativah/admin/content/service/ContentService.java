package com.gativah.admin.content.service;

import java.util.List;

import com.gativah.admin.content.dto.ContentRow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContentService {

    Page<ContentRow> list(List<String> types, String q, List<String> statuses, Pageable pageable);

    void takedown(Long actorAdminId, String type, Long id, String reason);
}

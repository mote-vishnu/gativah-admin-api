package com.gativah.admin.legal.service;

import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.LegalRequestDetail;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.UpdateLegalRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LegalService {

    Page<LegalRequestSummary> list(String status, Pageable pageable);

    LegalRequestDetail detail(Long id);

    LegalRequestSummary create(Long actorAdminId, CreateLegalRequest req);

    LegalRequestSummary update(Long actorAdminId, Long id, UpdateLegalRequest req);

    DisclosureRow recordDisclosure(Long actorAdminId, Long requestId, RecordDisclosureRequest req);
}

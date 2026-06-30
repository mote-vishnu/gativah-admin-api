package com.gativah.admin.legal.service;

import java.util.List;

import com.gativah.admin.legal.dto.AddCorrespondenceRequest;
import com.gativah.admin.legal.dto.ApprovalRequest;
import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.CreateTaskRequest;
import com.gativah.admin.legal.dto.DisclosureRegisterRow;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.LegalRequestDetail;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.LegalStats;
import com.gativah.admin.legal.dto.LegalTaskListRow;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.UpdateLegalRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LegalService {

    Page<LegalRequestSummary> list(String q, List<String> statuses, List<String> types,
                                   boolean overdueOnly, Pageable pageable);

    LegalStats stats();

    Page<LegalTaskListRow> openTasks(Pageable pageable);

    Page<DisclosureRegisterRow> disclosureRegister(Pageable pageable);

    LegalRequestDetail detail(Long id);

    LegalRequestSummary create(Long actorAdminId, CreateLegalRequest req);

    LegalRequestSummary update(Long actorAdminId, Long id, UpdateLegalRequest req);

    void approve(Long actorAdminId, Long id, ApprovalRequest req);

    DisclosureRow recordDisclosure(Long actorAdminId, Long requestId, RecordDisclosureRequest req);

    void addTask(Long actorAdminId, Long requestId, CreateTaskRequest req);

    void completeTask(Long actorAdminId, Long taskId);

    void addCorrespondence(Long actorAdminId, Long requestId, AddCorrespondenceRequest req);
}

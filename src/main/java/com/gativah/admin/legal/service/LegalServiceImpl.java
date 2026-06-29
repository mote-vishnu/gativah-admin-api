package com.gativah.admin.legal.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.legal.dto.AddCorrespondenceRequest;
import com.gativah.admin.legal.dto.ApprovalRequest;
import com.gativah.admin.legal.dto.CorrespondenceRow;
import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.CreateTaskRequest;
import com.gativah.admin.legal.dto.CustodyEventRow;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.LegalRequestDetail;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.TaskRow;
import com.gativah.admin.legal.dto.UpdateLegalRequest;
import com.gativah.admin.legal.model.LegalCorrespondence;
import com.gativah.admin.legal.model.LegalCustodyEvent;
import com.gativah.admin.legal.model.LegalDisclosure;
import com.gativah.admin.legal.model.LegalRequest;
import com.gativah.admin.legal.model.LegalTask;
import com.gativah.admin.legal.repo.LegalCorrespondenceRepository;
import com.gativah.admin.legal.repo.LegalCustodyEventRepository;
import com.gativah.admin.legal.repo.LegalDisclosureRepository;
import com.gativah.admin.legal.repo.LegalRequestRepository;
import com.gativah.admin.legal.repo.LegalTaskRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LegalServiceImpl implements LegalService {

    private static final Set<String> VALID_STATUSES = Set.of(
            LegalRequest.STATUS_RECEIVED, LegalRequest.STATUS_UNDER_REVIEW,
            LegalRequest.STATUS_ACTIONED, LegalRequest.STATUS_REJECTED, LegalRequest.STATUS_CLOSED);

    private final LegalRequestRepository requests;
    private final LegalDisclosureRepository disclosures;
    private final LegalTaskRepository tasks;
    private final LegalCorrespondenceRepository correspondence;
    private final LegalCustodyEventRepository custody;
    private final AuditService audit;

    public LegalServiceImpl(LegalRequestRepository requests, LegalDisclosureRepository disclosures,
                            LegalTaskRepository tasks, LegalCorrespondenceRepository correspondence,
                            LegalCustodyEventRepository custody, AuditService audit) {
        this.requests = requests;
        this.disclosures = disclosures;
        this.tasks = tasks;
        this.correspondence = correspondence;
        this.custody = custody;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LegalRequestSummary> list(List<String> statuses, Pageable pageable) {
        Page<LegalRequest> page = (statuses == null || statuses.isEmpty())
                ? requests.findAllByOrderByReceivedAtDesc(pageable)
                : requests.findByStatusInOrderByReceivedAtDesc(statuses, pageable);
        return page.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public LegalRequestDetail detail(Long id) {
        LegalRequest r = requireRequest(id);
        List<DisclosureRow> disc = disclosures.findByRequestIdOrderByDisclosedAtDesc(id).stream()
                .map(LegalServiceImpl::toRow).toList();
        List<TaskRow> taskRows = tasks.findByRequestIdOrderByCreatedAtDesc(id).stream()
                .map(LegalServiceImpl::toTaskRow).toList();
        List<CorrespondenceRow> corrRows = correspondence.findByRequestIdOrderByCreatedAtDesc(id).stream()
                .map(LegalServiceImpl::toCorrRow).toList();
        List<CustodyEventRow> custodyRows = custody.findByRequestIdOrderByCreatedAtDesc(id).stream()
                .map(LegalServiceImpl::toCustodyRow).toList();
        return new LegalRequestDetail(r.getId(), r.getReference(), r.getRequestType(),
                r.getRequestingAuthority(), r.getSubjectUserId(), r.getScope(), r.getStatus(),
                r.getReceivedAt(), r.getDueAt(), r.getNotes(),
                r.getApprovalStatus(), r.getApprovedBy(), r.getApprovedAt(), r.getApprovalNote(),
                r.getCreatedBy(), r.getCreatedAt(), r.getUpdatedAt(),
                disc, taskRows, corrRows, custodyRows);
    }

    @Override
    @Transactional
    public LegalRequestSummary create(Long actorAdminId, CreateLegalRequest req) {
        String reference = req.reference().trim();
        if (requests.existsByReferenceIgnoreCase(reference)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A request with this reference already exists");
        }
        LegalRequest r = new LegalRequest();
        r.setReference(reference);
        r.setRequestType(req.requestType().trim());
        r.setRequestingAuthority(req.requestingAuthority().trim());
        r.setSubjectUserId(req.subjectUserId());
        r.setScope(trimToNull(req.scope()));
        r.setDueAt(req.dueAt());
        r.setNotes(trimToNull(req.notes()));
        r.setStatus(LegalRequest.STATUS_RECEIVED);
        r.setCreatedBy(actorAdminId);
        r = requests.save(r);
        logCustody(r.getId(), "RECEIVED", reference + " from " + r.getRequestingAuthority(), actorAdminId);
        audit.record(actorAdminId, "LEGAL_REQUEST_CREATE", "LEGAL_REQUEST", String.valueOf(r.getId()),
                r.getRequestType() + " " + reference + " from " + r.getRequestingAuthority(), null, null);
        return toSummary(r);
    }

    @Override
    @Transactional
    public LegalRequestSummary update(Long actorAdminId, Long id, UpdateLegalRequest req) {
        LegalRequest r = requireRequest(id);
        if (req.status() != null) {
            String status = req.status().trim();
            if (!VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
            }
            if (!status.equals(r.getStatus())) {
                logCustody(id, "STATUS_CHANGE", r.getStatus() + " → " + status, actorAdminId);
            }
            r.setStatus(status);
        }
        if (req.notes() != null) {
            r.setNotes(trimToNull(req.notes()));
        }
        r = requests.save(r);
        audit.record(actorAdminId, "LEGAL_REQUEST_UPDATE", "LEGAL_REQUEST", String.valueOf(id),
                "status=" + r.getStatus(), null, null);
        return toSummary(r);
    }

    @Override
    @Transactional
    public void approve(Long actorAdminId, Long id, ApprovalRequest req) {
        LegalRequest r = requireRequest(id);
        r.setApprovalStatus(req.approve() ? LegalRequest.APPROVAL_APPROVED : LegalRequest.APPROVAL_REJECTED);
        r.setApprovedBy(actorAdminId);
        r.setApprovedAt(LocalDateTime.now());
        r.setApprovalNote(trimToNull(req.note()));
        if (!req.approve()) {
            r.setStatus(LegalRequest.STATUS_REJECTED);
        }
        requests.save(r);
        logCustody(id, req.approve() ? "APPROVED" : "REJECTED", trimToNull(req.note()), actorAdminId);
        audit.record(actorAdminId, "LEGAL_APPROVAL", "LEGAL_REQUEST", String.valueOf(id),
                req.approve() ? "approved" : "rejected", null, null);
    }

    @Override
    @Transactional
    public DisclosureRow recordDisclosure(Long actorAdminId, Long requestId, RecordDisclosureRequest req) {
        LegalRequest r = requireRequest(requestId);
        if (LegalRequest.STATUS_REJECTED.equals(r.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot disclose under a rejected request");
        }
        if (!LegalRequest.APPROVAL_APPROVED.equals(r.getApprovalStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request must be approved before disclosure");
        }
        LegalDisclosure d = new LegalDisclosure();
        d.setRequestId(requestId);
        d.setDisclosedBy(actorAdminId);
        d.setRecipient(req.recipient().trim());
        d.setDataCategories(req.dataCategories().trim());
        d.setJustification(req.justification().trim());
        d = disclosures.save(d);

        if (LegalRequest.STATUS_RECEIVED.equals(r.getStatus())
                || LegalRequest.STATUS_UNDER_REVIEW.equals(r.getStatus())) {
            r.setStatus(LegalRequest.STATUS_ACTIONED);
            requests.save(r);
        }
        logCustody(requestId, "DISCLOSED", "[" + d.getDataCategories() + "] to " + d.getRecipient(), actorAdminId);
        audit.record(actorAdminId, "LEGAL_DISCLOSURE", "LEGAL_REQUEST", String.valueOf(requestId),
                "disclosed [" + d.getDataCategories() + "] to " + d.getRecipient(), null, null);
        return toRow(d);
    }

    @Override
    @Transactional
    public void addTask(Long actorAdminId, Long requestId, CreateTaskRequest req) {
        requireRequest(requestId);
        LegalTask t = new LegalTask();
        t.setRequestId(requestId);
        t.setTitle(req.title().trim());
        t.setAssigneeAdminId(req.assigneeAdminId());
        t.setDueAt(req.dueAt());
        t.setCreatedBy(actorAdminId);
        tasks.save(t);
        audit.record(actorAdminId, "LEGAL_TASK_ADD", "LEGAL_REQUEST", String.valueOf(requestId), req.title(), null, null);
    }

    @Override
    @Transactional
    public void completeTask(Long actorAdminId, Long taskId) {
        LegalTask t = tasks.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));
        t.setStatus(LegalTask.STATUS_DONE);
        t.setCompletedAt(LocalDateTime.now());
        tasks.save(t);
        audit.record(actorAdminId, "LEGAL_TASK_DONE", "LEGAL_REQUEST", String.valueOf(t.getRequestId()), t.getTitle(), null, null);
    }

    @Override
    @Transactional
    public void addCorrespondence(Long actorAdminId, Long requestId, AddCorrespondenceRequest req) {
        requireRequest(requestId);
        LegalCorrespondence c = new LegalCorrespondence();
        c.setRequestId(requestId);
        c.setDirection(req.direction().trim());
        c.setChannel(trimToNull(req.channel()));
        c.setSummary(req.summary().trim());
        c.setCreatedBy(actorAdminId);
        correspondence.save(c);
        logCustody(requestId, "NOTE", req.direction() + ": " + truncate(req.summary()), actorAdminId);
        audit.record(actorAdminId, "LEGAL_CORRESPONDENCE", "LEGAL_REQUEST", String.valueOf(requestId), req.direction(), null, null);
    }

    private LegalRequest requireRequest(Long id) {
        return requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal request not found: " + id));
    }

    private void logCustody(Long requestId, String event, String detail, Long actorAdminId) {
        LegalCustodyEvent e = new LegalCustodyEvent();
        e.setRequestId(requestId);
        e.setEvent(event);
        e.setDetail(detail);
        e.setActorAdminId(actorAdminId);
        custody.save(e);
    }

    private LegalRequestSummary toSummary(LegalRequest r) {
        return new LegalRequestSummary(r.getId(), r.getReference(), r.getRequestType(),
                r.getRequestingAuthority(), r.getSubjectUserId(), r.getStatus(),
                r.getReceivedAt(), r.getDueAt(), disclosures.countByRequestId(r.getId()));
    }

    private static DisclosureRow toRow(LegalDisclosure d) {
        return new DisclosureRow(d.getId(), d.getDisclosedBy(), d.getRecipient(),
                d.getDataCategories(), d.getJustification(), d.getDisclosedAt());
    }

    private static TaskRow toTaskRow(LegalTask t) {
        return new TaskRow(t.getId(), t.getTitle(), t.getStatus(), t.getAssigneeAdminId(),
                t.getDueAt(), t.getCreatedBy(), t.getCreatedAt(), t.getCompletedAt());
    }

    private static CorrespondenceRow toCorrRow(LegalCorrespondence c) {
        return new CorrespondenceRow(c.getId(), c.getDirection(), c.getChannel(), c.getSummary(),
                c.getCreatedBy(), c.getCreatedAt());
    }

    private static CustodyEventRow toCustodyRow(LegalCustodyEvent e) {
        return new CustodyEventRow(e.getId(), e.getEvent(), e.getDetail(), e.getActorAdminId(), e.getCreatedAt());
    }

    private static String truncate(String s) {
        return s.length() <= 80 ? s : s.substring(0, 80) + "…";
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

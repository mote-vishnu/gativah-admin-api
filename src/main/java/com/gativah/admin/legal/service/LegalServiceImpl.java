package com.gativah.admin.legal.service;

import java.util.List;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.LegalRequestDetail;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.UpdateLegalRequest;
import com.gativah.admin.legal.model.LegalDisclosure;
import com.gativah.admin.legal.model.LegalRequest;
import com.gativah.admin.legal.repo.LegalDisclosureRepository;
import com.gativah.admin.legal.repo.LegalRequestRepository;

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
    private final AuditService audit;

    public LegalServiceImpl(LegalRequestRepository requests, LegalDisclosureRepository disclosures,
                            AuditService audit) {
        this.requests = requests;
        this.disclosures = disclosures;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LegalRequestSummary> list(String status, Pageable pageable) {
        Page<LegalRequest> page = status == null
                ? requests.findAllByOrderByReceivedAtDesc(pageable)
                : requests.findByStatusOrderByReceivedAtDesc(status, pageable);
        return page.map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public LegalRequestDetail detail(Long id) {
        LegalRequest r = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal request not found: " + id));
        List<DisclosureRow> rows = disclosures.findByRequestIdOrderByDisclosedAtDesc(id).stream()
                .map(LegalServiceImpl::toRow)
                .toList();
        return new LegalRequestDetail(r.getId(), r.getReference(), r.getRequestType(),
                r.getRequestingAuthority(), r.getSubjectUserId(), r.getScope(), r.getStatus(),
                r.getReceivedAt(), r.getDueAt(), r.getNotes(), r.getCreatedBy(),
                r.getCreatedAt(), r.getUpdatedAt(), rows);
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
        audit.record(actorAdminId, "LEGAL_REQUEST_CREATE", "LEGAL_REQUEST", String.valueOf(r.getId()),
                r.getRequestType() + " " + reference + " from " + r.getRequestingAuthority(), null, null);
        return toSummary(r);
    }

    @Override
    @Transactional
    public LegalRequestSummary update(Long actorAdminId, Long id, UpdateLegalRequest req) {
        LegalRequest r = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal request not found: " + id));
        if (req.status() != null) {
            String status = req.status().trim();
            if (!VALID_STATUSES.contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
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
    public DisclosureRow recordDisclosure(Long actorAdminId, Long requestId, RecordDisclosureRequest req) {
        LegalRequest r = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Legal request not found: " + requestId));
        if (LegalRequest.STATUS_REJECTED.equals(r.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot disclose under a rejected request");
        }
        LegalDisclosure d = new LegalDisclosure();
        d.setRequestId(requestId);
        d.setDisclosedBy(actorAdminId);
        d.setRecipient(req.recipient().trim());
        d.setDataCategories(req.dataCategories().trim());
        d.setJustification(req.justification().trim());
        d = disclosures.save(d);

        // A disclosure means the request has been acted on.
        if (LegalRequest.STATUS_RECEIVED.equals(r.getStatus())
                || LegalRequest.STATUS_UNDER_REVIEW.equals(r.getStatus())) {
            r.setStatus(LegalRequest.STATUS_ACTIONED);
            requests.save(r);
        }
        audit.record(actorAdminId, "LEGAL_DISCLOSURE", "LEGAL_REQUEST", String.valueOf(requestId),
                "disclosed [" + d.getDataCategories() + "] to " + d.getRecipient(), null, null);
        return toRow(d);
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

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

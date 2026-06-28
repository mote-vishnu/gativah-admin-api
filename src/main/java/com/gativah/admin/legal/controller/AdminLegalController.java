package com.gativah.admin.legal.controller;

import java.util.List;

import jakarta.validation.Valid;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.legal.dto.CreateLegalRequest;
import com.gativah.admin.legal.dto.DisclosureRow;
import com.gativah.admin.legal.dto.LegalRequestDetail;
import com.gativah.admin.legal.dto.LegalRequestSummary;
import com.gativah.admin.legal.dto.RecordDisclosureRequest;
import com.gativah.admin.legal.dto.UpdateLegalRequest;
import com.gativah.admin.legal.service.LegalService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Legal requests + disclosure ledger. Gated by LEGAL:VIEW (read) / LEGAL:EDIT (write). */
@RestController
public class AdminLegalController {

    private final LegalService service;

    public AdminLegalController(LegalService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/legal/requests")
    @PreAuthorize("hasAuthority('LEGAL:VIEW')")
    public Page<LegalRequestSummary> requests(@RequestParam(required = false) List<String> status,
                                              @PageableDefault(size = 20) Pageable pageable) {
        return service.list(status, pageable);
    }

    @GetMapping("/api/v1/admin/legal/requests/{id}")
    @PreAuthorize("hasAuthority('LEGAL:VIEW')")
    public LegalRequestDetail request(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/api/v1/admin/legal/requests")
    @PreAuthorize("hasAuthority('LEGAL:EDIT')")
    public LegalRequestSummary create(@AuthenticationPrincipal AdminPrincipal principal,
                                      @Valid @RequestBody CreateLegalRequest req) {
        return service.create(principal.id(), req);
    }

    @PatchMapping("/api/v1/admin/legal/requests/{id}")
    @PreAuthorize("hasAuthority('LEGAL:EDIT')")
    public LegalRequestSummary update(@AuthenticationPrincipal AdminPrincipal principal,
                                      @PathVariable Long id, @RequestBody UpdateLegalRequest req) {
        return service.update(principal.id(), id, req);
    }

    @PostMapping("/api/v1/admin/legal/requests/{id}/disclosures")
    @PreAuthorize("hasAuthority('LEGAL:EDIT')")
    public DisclosureRow recordDisclosure(@AuthenticationPrincipal AdminPrincipal principal,
                                          @PathVariable Long id, @Valid @RequestBody RecordDisclosureRequest req) {
        return service.recordDisclosure(principal.id(), id, req);
    }
}

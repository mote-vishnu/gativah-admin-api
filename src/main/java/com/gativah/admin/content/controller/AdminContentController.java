package com.gativah.admin.content.controller;

import java.util.List;

import com.gativah.admin.auth.security.AdminPrincipal;
import com.gativah.admin.content.dto.ContentDetail;
import com.gativah.admin.content.dto.ContentReportsResponse;
import com.gativah.admin.content.dto.ContentRow;
import com.gativah.admin.content.dto.ContentStats;
import com.gativah.admin.content.dto.StoryRow;
import com.gativah.admin.content.dto.TakedownContentRequest;
import com.gativah.admin.content.service.ContentService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Content browser + proactive takedown. CONTENT:VIEW (read) / CONTENT:EDIT (takedown). */
@RestController
public class AdminContentController {

    private final ContentService service;

    public AdminContentController(ContentService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/admin/content")
    @PreAuthorize("hasAuthority('CONTENT:VIEW')")
    public Page<ContentRow> content(@RequestParam(required = false) List<String> type,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) List<String> status,
                                    @RequestParam(required = false, defaultValue = "false") boolean reported,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return service.list(type, q, status, reported, pageable);
    }

    @GetMapping("/api/v1/admin/content/stats")
    @PreAuthorize("hasAuthority('CONTENT:VIEW')")
    public ContentStats stats() {
        return service.stats();
    }

    @GetMapping("/api/v1/admin/content/{type}/{id}/reports")
    @PreAuthorize("hasAuthority('CONTENT:VIEW')")
    public ContentReportsResponse reports(@PathVariable String type, @PathVariable Long id) {
        return new ContentReportsResponse(service.reportsFor(type, id));
    }

    @GetMapping("/api/v1/admin/content/{type}/{id}/detail")
    @PreAuthorize("hasAuthority('CONTENT:VIEW')")
    public ContentDetail detail(@PathVariable String type, @PathVariable Long id) {
        return service.contentDetail(type, id);
    }

    @GetMapping("/api/v1/admin/content/stories")
    @PreAuthorize("hasAuthority('CONTENT:VIEW')")
    public Page<StoryRow> stories(@RequestParam(required = false) String q,
                                  @PageableDefault(size = 20) Pageable pageable) {
        return service.stories(q, pageable);
    }

    @PostMapping("/api/v1/admin/content/{type}/{id}/takedown")
    @PreAuthorize("hasAuthority('CONTENT:EDIT')")
    public ResponseEntity<Void> takedown(@AuthenticationPrincipal AdminPrincipal principal,
                                         @PathVariable String type, @PathVariable Long id,
                                         @RequestBody TakedownContentRequest req) {
        service.takedown(principal.id(), type, id, req.reason());
        return ResponseEntity.noContent().build();
    }
}

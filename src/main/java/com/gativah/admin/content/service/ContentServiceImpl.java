package com.gativah.admin.content.service;

import java.util.List;
import java.util.Set;

import com.gativah.admin.audit.service.AuditService;
import com.gativah.admin.client.PacegritInternalClient;
import com.gativah.admin.content.dto.ContentRow;
import com.gativah.admin.content.dto.StoryRow;
import com.gativah.admin.content.query.ContentQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContentServiceImpl implements ContentService {

    private static final Set<String> TYPES = Set.of("POST", "COMMENT");

    private final ContentQuery query;
    private final PacegritInternalClient internal;
    private final AuditService audit;

    public ContentServiceImpl(ContentQuery query, PacegritInternalClient internal, AuditService audit) {
        this.query = query;
        this.internal = internal;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContentRow> list(List<String> types, String q, List<String> statuses, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim() + "%";
        return query.search(normalizeTypes(types), like, removedFlags(statuses), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoryRow> stories(String q, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim() + "%";
        return query.stories(like, pageable);
    }

    @Override
    public void takedown(Long actorAdminId, String type, Long id, String reason) {
        String ct = normalizeType(type);
        if (ct == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported content type: " + type);
        }
        internal.takedown(actorAdminId, ct, id, reason);
        audit.record(actorAdminId, "CONTENT_TAKEDOWN", ct, String.valueOf(id), reason, null, null);
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String t = type.toUpperCase();
        return TYPES.contains(t) ? t : null;
    }

    /** Valid, upper-cased, de-duped content types — or null when none apply. */
    private List<String> normalizeTypes(List<String> types) {
        if (types == null) {
            return null;
        }
        List<String> out = types.stream()
                .map(this::normalizeType)
                .filter(t -> t != null)
                .distinct()
                .toList();
        return out.isEmpty() ? null : out;
    }

    /** Map status names to the removed flag: removed→true, active→false. Null when none. */
    private List<Boolean> removedFlags(List<String> statuses) {
        if (statuses == null) {
            return null;
        }
        List<Boolean> out = statuses.stream()
                .map(s -> s == null ? null : s.toLowerCase())
                .map(s -> "removed".equals(s) ? Boolean.TRUE : "active".equals(s) ? Boolean.FALSE : null)
                .filter(b -> b != null)
                .distinct()
                .toList();
        return out.isEmpty() ? null : out;
    }
}

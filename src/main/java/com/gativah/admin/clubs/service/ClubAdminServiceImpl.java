package com.gativah.admin.clubs.service;

import java.util.List;

import com.gativah.admin.clubs.dto.ClubDetail;
import com.gativah.admin.clubs.dto.ClubSummary;
import com.gativah.admin.clubs.query.ClubQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ClubAdminServiceImpl implements ClubAdminService {

    private final ClubQuery query;

    public ClubAdminServiceImpl(ClubQuery query) {
        this.query = query;
    }

    @Override
    public Page<ClubSummary> list(String q, List<String> visibilities, List<String> statuses, Pageable pageable) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.trim() + "%";
        return query.search(like, upper(visibilities), removedFlags(statuses), pageable);
    }

    @Override
    public ClubDetail detail(Long id) {
        ClubDetail detail = query.detail(id);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found: " + id);
        }
        return detail;
    }

    /** Upper-cased, de-duped visibilities — or null when none apply. */
    private List<String> upper(List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> out = values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.toUpperCase())
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

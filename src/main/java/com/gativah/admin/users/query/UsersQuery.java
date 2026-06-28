package com.gativah.admin.users.query;

import java.util.List;

import com.gativah.admin.users.dto.UserDetail;
import com.gativah.admin.users.dto.UserSummary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsersQuery {

    Page<UserSummary> search(String q, List<String> statuses, Pageable pageable);

    /** Full profile (subscription + sanctions) or null if the user doesn't exist. */
    UserDetail detail(Long id);
}

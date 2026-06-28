package com.gativah.admin.moderation.repo;

import com.gativah.admin.moderation.model.Appeal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

    Page<Appeal> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Appeal> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

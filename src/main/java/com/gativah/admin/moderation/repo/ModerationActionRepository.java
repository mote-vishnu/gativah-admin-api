package com.gativah.admin.moderation.repo;

import com.gativah.admin.moderation.model.ModerationAction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {

    Page<ModerationAction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

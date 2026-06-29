package com.gativah.admin.legal.repo;

import java.util.List;

import com.gativah.admin.legal.model.LegalTask;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalTaskRepository extends JpaRepository<LegalTask, Long> {

    List<LegalTask> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}

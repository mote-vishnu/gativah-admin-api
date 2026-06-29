package com.gativah.admin.legal.repo;

import java.util.List;

import com.gativah.admin.legal.model.LegalCustodyEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCustodyEventRepository extends JpaRepository<LegalCustodyEvent, Long> {

    List<LegalCustodyEvent> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}

package com.gativah.admin.legal.repo;

import java.util.List;

import com.gativah.admin.legal.model.LegalRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long> {

    Page<LegalRequest> findByStatusInOrderByReceivedAtDesc(List<String> statuses, Pageable pageable);

    Page<LegalRequest> findAllByOrderByReceivedAtDesc(Pageable pageable);

    boolean existsByReferenceIgnoreCase(String reference);
}

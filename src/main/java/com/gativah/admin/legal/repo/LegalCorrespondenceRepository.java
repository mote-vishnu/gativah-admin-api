package com.gativah.admin.legal.repo;

import java.util.List;

import com.gativah.admin.legal.model.LegalCorrespondence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalCorrespondenceRepository extends JpaRepository<LegalCorrespondence, Long> {

    List<LegalCorrespondence> findByRequestIdOrderByCreatedAtDesc(Long requestId);
}

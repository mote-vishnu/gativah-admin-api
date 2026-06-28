package com.gativah.admin.legal.repo;

import java.util.List;

import com.gativah.admin.legal.model.LegalDisclosure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalDisclosureRepository extends JpaRepository<LegalDisclosure, Long> {

    List<LegalDisclosure> findByRequestIdOrderByDisclosedAtDesc(Long requestId);

    long countByRequestId(Long requestId);
}

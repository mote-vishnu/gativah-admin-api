package com.gativah.admin.auth.repo;

import java.util.List;

import com.gativah.admin.auth.model.AdminFeature;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminFeatureRepository extends JpaRepository<AdminFeature, Long> {

    List<AdminFeature> findAllByOrderBySortOrderAsc();
}

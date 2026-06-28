package com.gativah.admin.moderation.repo;

import com.gativah.admin.moderation.model.ContentReport;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
}

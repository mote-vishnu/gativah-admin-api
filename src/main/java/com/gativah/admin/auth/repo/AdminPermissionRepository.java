package com.gativah.admin.auth.repo;

import java.util.List;

import com.gativah.admin.auth.model.AdminPermission;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminPermissionRepository extends JpaRepository<AdminPermission, Long> {

    List<AdminPermission> findAllByOrderByCodeAsc();
}

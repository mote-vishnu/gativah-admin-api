package com.gativah.admin.auth.repo;

import java.util.List;
import java.util.Optional;

import com.gativah.admin.auth.model.AdminRole;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRoleRepository extends JpaRepository<AdminRole, Long> {

    Optional<AdminRole> findByName(String name);

    List<AdminRole> findAllByOrderByNameAsc();
}

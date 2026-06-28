package com.gativah.admin.auth.repo;

import java.util.Optional;

import com.gativah.admin.auth.model.AdminUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<AdminUser> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByRoles_Id(Long roleId);
}

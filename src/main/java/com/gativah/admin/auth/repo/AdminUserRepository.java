package com.gativah.admin.auth.repo;

import java.util.Optional;

import com.gativah.admin.auth.model.AdminUser;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByEmailIgnoreCase(String email);
}

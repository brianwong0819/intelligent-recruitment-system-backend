// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/admin/AdminRepository.java

package com.event.recruitment.intelligent_recruitment_system.repository.admin;

import com.event.recruitment.intelligent_recruitment_system.model.entity.admin.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByEmail(String email);
}
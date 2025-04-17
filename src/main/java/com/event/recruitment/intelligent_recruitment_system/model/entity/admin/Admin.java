// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/admin/Admin.java

package com.event.recruitment.intelligent_recruitment_system.model.entity.admin;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Builder
    public Admin(Long id, String username, String password, String email, Boolean isDeleted) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.isDeleted = isDeleted;
        this.createdAt = LocalDateTime.now(); // Ensure createdAt is always set
    }
}
// src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/job/Jobs.java

package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobTitleType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.SalaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Jobs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Projects project;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_title_type")
    private JobTitleType jobTitleType;

    @Column(name = "job_scope", nullable = false, columnDefinition = "TEXT")
    private String jobScope;

    @Column(name = "requirements", nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false)
    private SalaryType salaryType;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatusType status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobSchedule> jobSchedules;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    private List<JobLocation> jobLocations;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatusType.OPEN;
        }
        if (salaryType == null) {
            salaryType = SalaryType.PER_HOUR;
        }
    }
}
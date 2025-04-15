// src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/job/ViewedJob.java

package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "viewed_jobs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "job_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 1;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt = LocalDateTime.now();
}
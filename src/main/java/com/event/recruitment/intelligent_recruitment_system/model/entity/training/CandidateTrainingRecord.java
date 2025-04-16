// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/training/CandidateTrainingRecord.java

package com.event.recruitment.intelligent_recruitment_system.model.entity.training;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_training_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "job_id", "training_material_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateTrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "training_material_id", nullable = false)
    private Long trainingMaterialId;

    @Column(name = "first_viewed_at")
    private LocalDateTime firstViewedAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completion_date")
    private LocalDateTime completionDate;

    // Constructor for creating a new record
    public CandidateTrainingRecord(Long candidateId, Long jobId, Long trainingMaterialId) {
        this.candidateId = candidateId;
        this.jobId = jobId;
        this.trainingMaterialId = trainingMaterialId;
        this.viewCount = 0;
        this.isCompleted = false;
    }

    /**
     * Record a new view of the training material
     */
    public void recordView() {
        LocalDateTime now = LocalDateTime.now();
        if (this.firstViewedAt == null) {
            this.firstViewedAt = now;
        }
        this.lastViewedAt = now;
        this.viewCount++;
    }

    /**
     * Mark the training as completed
     */
    public void markAsCompleted() {
        this.isCompleted = true;
        this.completionDate = LocalDateTime.now();
    }
}
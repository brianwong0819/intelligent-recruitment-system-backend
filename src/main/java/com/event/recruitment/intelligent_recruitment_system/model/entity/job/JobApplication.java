package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "job_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"job_location_id", "candidate_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_location_id", nullable = false)
    private JobLocation jobLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidates candidate;

    @Column(name = "application_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    @Column(name = "application_date")
    @CreationTimestamp
    private LocalDateTime applicationDate;

    @Column(name = "notes")
    private String notes;

    @Column(name = "hired_date")
    private LocalDateTime hiredDate;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "application_group_id")
    private String applicationGroupId;

    // Generate a new group ID
    public static String generateGroupId() {
        return UUID.randomUUID().toString();
    }

    @Column(name = "distance_to_candidate")
    private Double distanceToCandidate;

    @Column(name = "withdrawal_reason")
    private String withdrawalReason;

    public enum ApplicationStatus {
        PENDING,
        UNDER_REVIEW,
        SHORTLISTED,
        HIRED,
        REJECTED,
        CANCELLED,
        WITHDRAWN,
        BACKUP,
        COMPLETED
    }
}
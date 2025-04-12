package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_locations",
        indexes = {
                @Index(name = "idx_job_location", columnList = "location_id"),
                @Index(name = "idx_job_location_schedule_date", columnList = "schedule_date_id")
        }
)
public class JobLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Jobs job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_date_id")
    private JobScheduleDate jobScheduleDate;

    @Column(name = "positions_needed")
    private Integer positionsNeeded;

    @Column(name = "positions_filled", columnDefinition = "integer default 0")
    private Integer positionsFilled = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private JobLocationStatus status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = JobLocationStatus.OPEN;
        }
        if (positionsFilled == null) {
            positionsFilled = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
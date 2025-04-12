// src/main/java/com/event/recruitment/intelligent_recruitment_system/model/entity/job/JobSchedule.java

package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_schedules",
        indexes = {
                @Index(name = "idx_job_schedule", columnList = "job_id"),
                @Index(name = "idx_schedule_dates", columnList = "start_date,end_date")
        }
)
public class JobSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Jobs job;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "hours_of_rest_time", precision = 6, scale = 2)
    private BigDecimal hoursOfRestTime;

    @Column(name = "num_positions", nullable = false)
    private Integer numPositions;

    @OneToMany(mappedBy = "jobSchedule",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<JobScheduleDate> scheduleDates = new ArrayList<>();

    // Helper methods remain the same
    public void addScheduleDate(JobScheduleDate scheduleDate) {
        if (scheduleDates == null) {
            scheduleDates = new ArrayList<>();
        }
        scheduleDates.add(scheduleDate);
        scheduleDate.setJobSchedule(this);
    }

    public void clearScheduleDates() {
        if (this.scheduleDates != null) {
            this.scheduleDates.clear();
        }
    }
}
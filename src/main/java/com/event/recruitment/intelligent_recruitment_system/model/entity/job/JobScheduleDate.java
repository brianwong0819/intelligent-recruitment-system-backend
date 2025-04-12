package com.event.recruitment.intelligent_recruitment_system.model.entity.job;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_schedule_dates",
        indexes = {
                @Index(name = "idx_work_date", columnList = "work_date")
        }
)
public class JobScheduleDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private JobSchedule jobSchedule;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @OneToMany(mappedBy = "jobScheduleDate",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<JobLocation> jobLocations = new ArrayList<>();

    // Helper method to add job location
    public void addJobLocation(JobLocation jobLocation) {
        if (jobLocations == null) {
            jobLocations = new ArrayList<>();
        }
        jobLocations.add(jobLocation);
        jobLocation.setJobScheduleDate(this);
    }

    // Helper method to clear job locations
    public void clearJobLocations() {
        if (this.jobLocations != null) {
            this.jobLocations.clear();
        }
    }
}
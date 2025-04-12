// src/main/java/com/event/recruitment/intelligent_recruitment_system/repository/job/JobSpecification.java

package com.event.recruitment.intelligent_recruitment_system.repository.job;

import com.event.recruitment.intelligent_recruitment_system.dto.request.job.JobListFilterRequest;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.ListingTimeFilter;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    /**
     * Creates a specification for filtering jobs based on provided criteria
     *
     * @param filter Filter criteria for jobs
     * @return Specification to be used with JPA repositories
     */
    public static Specification<Jobs> getJobsWithFilters(JobListFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Base condition: status = OPEN
            predicates.add(criteriaBuilder.equal(root.get("status"), JobStatusType.OPEN));

            // Apply JobTitleType filter
            if (filter.getJobTitleType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("jobTitleType"), filter.getJobTitleType()));
            }

            // Apply salary range filter
            if (filter.getMinSalary() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salary"), filter.getMinSalary()));
            }

            if (filter.getMaxSalary() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salary"), filter.getMaxSalary()));
            }

            // Apply listing time filter
            if (filter.getListingTime() != null && filter.getListingTime() != ListingTimeFilter.ALL) {
                LocalDateTime cutoffDate = calculateCutoffDate(filter.getListingTime());
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), cutoffDate));
            }

            // Apply location filters (city/state/coordinates)
            if (filter.getLocation() != null && !filter.getLocation().trim().isEmpty()) {
                // Join through all relationship tables to reach locations
                Join<Jobs, JobSchedule> scheduleJoin = root.join("jobSchedules");
                Join<JobSchedule, JobScheduleDate> scheduleDateJoin = scheduleJoin.join("scheduleDates");
                Join<JobScheduleDate, JobLocation> jobLocationJoin = scheduleDateJoin.join("jobLocations");
                Join<JobLocation, Location> locationJoin = jobLocationJoin.join("location");

                // Filter job_locations that are OPEN
                predicates.add(criteriaBuilder.equal(jobLocationJoin.get("status"), JobLocationStatus.OPEN));

                // Search in both city and state columns
                String locationSearch = filter.getLocation().trim().toUpperCase();

                Predicate cityPredicate = criteriaBuilder.like(
                        criteriaBuilder.upper(locationJoin.get("city")),
                        "%" + locationSearch + "%"
                );

                Predicate statePredicate = criteriaBuilder.like(
                        criteriaBuilder.upper(locationJoin.get("state")),
                        "%" + locationSearch + "%"
                );

                // Add location filter (match either city OR state)
                predicates.add(criteriaBuilder.or(cityPredicate, statePredicate));

                // Make the query distinct to avoid duplicates
                query.distinct(true);
            } else if (filter.getLatitude() != null && filter.getLongitude() != null && filter.getDistance() != null) {
                // Join through all relationship tables to reach locations
                Join<Jobs, JobSchedule> scheduleJoin = root.join("jobSchedules");
                Join<JobSchedule, JobScheduleDate> scheduleDateJoin = scheduleJoin.join("scheduleDates");
                Join<JobScheduleDate, JobLocation> jobLocationJoin = scheduleDateJoin.join("jobLocations");
                Join<JobLocation, Location> locationJoin = jobLocationJoin.join("location");

                // Filter job_locations that are OPEN
                predicates.add(criteriaBuilder.equal(jobLocationJoin.get("status"), JobLocationStatus.OPEN));

                // Calculate a bounding box to filter locations first (for performance)
                double lat = filter.getLatitude();
                double lon = filter.getLongitude();
                double distKm = filter.getDistance();

                // Approximate conversion of km to degrees (at the equator)
                // 1 degree of latitude is approximately 111 km
                // 1 degree of longitude varies with latitude, roughly 111 * cos(latitude in radians) km
                double latDiff = distKm / 111.0;
                double lonDiff = distKm / (111.0 * Math.cos(Math.toRadians(lat)));

                // Create a bounding box filter
                Predicate latPredicate = criteriaBuilder.between(
                        locationJoin.get("latitude"),
                        lat - latDiff,
                        lat + latDiff
                );

                Predicate lonPredicate = criteriaBuilder.between(
                        locationJoin.get("longitude"),
                        lon - lonDiff,
                        lon + lonDiff
                );

                predicates.add(criteriaBuilder.and(latPredicate, lonPredicate));

                // Make the query distinct to avoid duplicates
                query.distinct(true);
            }

            // Apply date range filter
            if (filter.getStartDate() != null || filter.getEndDate() != null) {
                LocalDate startDate = filter.getStartDate() != null ? filter.getStartDate() : LocalDate.now();
                LocalDate endDate = filter.getEndDate() != null ? filter.getEndDate() : LocalDate.now().plusYears(1);

                // Join to job_schedules and then to job_schedule_dates
                Join<Jobs, JobSchedule> scheduleJoin = root.join("jobSchedules");
                Join<JobSchedule, JobScheduleDate> scheduleDateJoin = scheduleJoin.join("scheduleDates");

                // Find jobs with dates that overlap with the requested range
                Predicate dateOverlap = criteriaBuilder.between(
                        scheduleDateJoin.get("workDate"),
                        startDate,
                        endDate
                );

                predicates.add(dateOverlap);

                // Make the query distinct to avoid duplicates
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Calculate cutoff date based on the time filter
     *
     * @param timeFilter Time filter type
     * @return LocalDateTime representing the cutoff date
     */
    private static LocalDateTime calculateCutoffDate(ListingTimeFilter timeFilter) {
        LocalDateTime now = LocalDateTime.now();

        return switch (timeFilter) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case LAST_3_DAYS -> now.minusDays(3);
            case LAST_WEEK -> now.minusWeeks(1);
            case LAST_MONTH -> now.minusMonths(1);
            case ALL -> LocalDateTime.of(2000, 1, 1, 0, 0); // A date in the distant past
        };
    }
}
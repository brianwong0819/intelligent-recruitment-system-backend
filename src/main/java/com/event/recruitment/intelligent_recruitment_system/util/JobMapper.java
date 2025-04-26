package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for mapping Job entities to DTOs
 */
@Component
public class JobMapper {

    /**
     * Maps a job entity to a summary DTO
     *
     * @param job The job entity to map
     * @return A JobSummaryResponseDTO containing essential job information
     */
    public JobSummaryResponseDTO toSummaryDTO(Jobs job) {
        // Handle company name based on recruiter type
        String companyName;
        String originalCompanyName = job.getProject().getRecruiter().getCompanyName();
        RecruiterType recruiterType = job.getProject().getRecruiter().getRecruiterType();

        if ((originalCompanyName == null || originalCompanyName.trim().isEmpty()) ||
                RecruiterType.INDIVIDUAL.equals(recruiterType)) {
            companyName = job.getProject().getRecruiter().getRecruiterRepName();
        } else {
            companyName = originalCompanyName;
        }

        // Calculate job schedule information
        LocalDate earliestStartDate = null;
        LocalDate latestEndDate = null;
        LocalTime startTime = null;
        LocalTime endTime = null;
        int totalPositions = 0;
        int availablePositions = 0;
        Set<String> locationNames = new HashSet<>();

        if (job.getJobSchedules() != null && !job.getJobSchedules().isEmpty()) {
            for (JobSchedule schedule : job.getJobSchedules()) {
                // Update date range
                if (schedule.getStartDate() != null) {
                    if (earliestStartDate == null || schedule.getStartDate().isBefore(earliestStartDate)) {
                        earliestStartDate = schedule.getStartDate();
                    }
                }

                if (schedule.getEndDate() != null) {
                    if (latestEndDate == null || schedule.getEndDate().isAfter(latestEndDate)) {
                        latestEndDate = schedule.getEndDate();
                    }
                }

                // Use the first schedule's time information if not set yet
                if (startTime == null) {
                    startTime = schedule.getStartTime();
                }

                if (endTime == null) {
                    endTime = schedule.getEndTime();
                }

                // Count positions
                totalPositions += schedule.getNumPositions();

                // Process schedule dates and locations
                if (schedule.getScheduleDates() != null) {
                    for (JobScheduleDate date : schedule.getScheduleDates()) {
                        if (date.getJobLocations() != null) {
                            for (JobLocation jobLocation : date.getJobLocations()) {
                                // Collect location names
                                if (jobLocation.getLocation() != null) {
                                    locationNames.add(jobLocation.getLocation().getName());
                                }

                                // Calculate available positions
                                int positionsNeeded = jobLocation.getPositionsNeeded() != null ?
                                        jobLocation.getPositionsNeeded() : schedule.getNumPositions();

                                int positionsFilled = jobLocation.getPositionsFilled() != null ?
                                        jobLocation.getPositionsFilled() : 0;

                                availablePositions += (positionsNeeded - positionsFilled);
                            }
                        }
                    }
                }
            }
        }

        // Convert set to list for the response
        List<String> locations = new ArrayList<>(locationNames);

        return JobSummaryResponseDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .companyName(companyName)
                .companyLogoUrl(job.getProject().getRecruiter().getCompanyLogoUrl())
                .recruiterType(recruiterType)
                .locations(locations)
                .salary(job.getSalary())
                .salaryType(job.getSalaryType())
                .paymentTerms(job.getPaymentTerms())
                .benefits(job.getBenefits())
                .createdAt(job.getCreatedAt())
                .earliestStartDate(earliestStartDate)
                .latestEndDate(latestEndDate)
                .startTime(startTime)
                .endTime(endTime)
                .totalPositions(totalPositions)
                .availablePositions(availablePositions)
                .build();
    }

    /**
     * Calculate the distance to the closest location associated with a job
     *
     * @param job The job entity
     * @param latitude The reference latitude
     * @param longitude The reference longitude
     * @return The distance in kilometers to the closest location, or null if no locations
     */
    public Double calculateDistanceToClosestLocation(Jobs job, Double latitude, Double longitude) {
        List<Location> jobLocations = new ArrayList<>();

        // Collect all locations from job schedules
        if (job.getJobSchedules() != null) {
            for (JobSchedule schedule : job.getJobSchedules()) {
                if (schedule.getScheduleDates() != null) {
                    for (JobScheduleDate date : schedule.getScheduleDates()) {
                        if (date.getJobLocations() != null) {
                            for (JobLocation jobLocation : date.getJobLocations()) {
                                if (jobLocation.getLocation() != null) {
                                    jobLocations.add(jobLocation.getLocation());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (jobLocations.isEmpty()) {
            return null;
        }

        // Find the closest location
        Double minDistance = null;

        for (Location location : jobLocations) {
            if (location.getLatitude() != null && location.getLongitude() != null) {
                // Convert BigDecimal to double
                double locationLat = location.getLatitude().doubleValue();
                double locationLon = location.getLongitude().doubleValue();

                double distance = calculateHaversineDistance(
                        latitude, longitude,
                        locationLat, locationLon
                );

                if (minDistance == null || distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    /**
     * Calculate distance between two points using the Haversine formula
     *
     * @param lat1 Latitude of first point
     * @param lon1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lon2 Longitude of second point
     * @return Distance in kilometers
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // Earth's radius in kilometers
        final double R = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c; // Distance in kilometers
    }
}
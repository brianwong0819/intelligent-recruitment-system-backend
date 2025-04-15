package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.ApplicantSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.ApplicantsResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.ai.AIRating;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.ai.AIRatingRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationManagementService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AIRatingRepository aiRatingRepository;
    private final SecurityUtil securityUtil;

    /**
     * Retrieve all applicants for a specific job, grouped by application group ID
     * @param jobId The job ID to get applicants for
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Field to sort by (finalScore, applicationDate, etc.)
     * @param sortDir Sort direction (asc/desc)
     * @return Response with paginated applicants data
     */
    public Response<ApplicantsResponseDTO> getJobApplicants(
            Long jobId,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Validate job exists and belongs to the recruiter
            Optional<Jobs> jobOpt = jobRepository.findByIdWithAllDetails(jobId);
            if (jobOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "Job not found", null);
            }

            Jobs job = jobOpt.get();
            Projects project = job.getProject();
            Recruiters jobRecruiter = project.getRecruiter();

            // Security check - ensure the job belongs to the current recruiter
            if (!jobRecruiter.getUsername().equals(username)) {
                log.warn("Security violation: User {} attempted to access job {} owned by {}",
                        username, job.getId(), jobRecruiter.getUsername());
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view applicants for this job", null);
            }

            // Set up sorting
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;

            // Default sort by application date if not specified
            if (sortBy == null || sortBy.isEmpty()) {
                sortBy = "applicationDate";
                direction = Sort.Direction.DESC;
            }

            Sort sort = Sort.by(direction, sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Get all job applications for this job with complete details
            List<JobApplication> allApplications = jobApplicationRepository.findByJobIdWithFullDetails(jobId);

            if (allApplications.isEmpty()) {
                return new Response<>(HttpStatus.OK.value(),
                        "No applicants found for this job",
                        new ApplicantsResponseDTO(
                                new ArrayList<>(),
                                0L,
                                page,
                                size,
                                0));
            }

            // Group applications by applicationGroupId or by candidate if no group ID
            Map<String, List<JobApplication>> groupedApplications = new HashMap<>();

            for (JobApplication app : allApplications) {
                String groupKey = app.getApplicationGroupId() != null && !app.getApplicationGroupId().isEmpty()
                        ? app.getApplicationGroupId()
                        : "single_" + app.getCandidate().getId();

                if (!groupedApplications.containsKey(groupKey)) {
                    groupedApplications.put(groupKey, new ArrayList<>());
                }
                groupedApplications.get(groupKey).add(app);
            }

            // Collect unique location names with dates for each group
            Map<String, Set<LocalDateTime>> locationDatesMap = new LinkedHashMap<>();

            for (List<JobApplication> apps : groupedApplications.values()) {
                for (JobApplication app : apps) {
                    JobLocation jobLocation = app.getJobLocation();
                    String locationName = jobLocation.getLocation().getName();

                    // Get work date from schedule date if available
                    LocalDateTime workDate = getWorkDateForJobLocation(jobLocation);

                    // Use computeIfAbsent to create a new Set if the location doesn't exist
                    locationDatesMap.computeIfAbsent(locationName, k -> new LinkedHashSet<>()).add(workDate);
                }
            }

            // Convert Set of dates to a map with sorted dates for each location
            Map<String, List<LocalDateTime>> flattenedLocationDatesMap = locationDatesMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .sorted()
                                    .collect(Collectors.toList())
                    ));

            // Sort groups by the criteria
            List<Map.Entry<String, List<JobApplication>>> sortedGroups = new ArrayList<>(groupedApplications.entrySet());

            final Sort.Direction dir = direction;

            if (sortBy.equalsIgnoreCase("applicationDate")) {
                sortedGroups.sort((a, b) -> {
                    LocalDateTime dateA = getMaxDate(a.getValue());
                    LocalDateTime dateB = getMaxDate(b.getValue());
                    return dir == Sort.Direction.DESC ?
                            dateB.compareTo(dateA) : dateA.compareTo(dateB);
                });
            } else if (sortBy.equalsIgnoreCase("finalScore")) {
                sortedGroups.sort((a, b) -> {
                    BigDecimal scoreA = getGroupScore(a.getKey(), a.getValue().get(0).getId());
                    BigDecimal scoreB = getGroupScore(b.getKey(), b.getValue().get(0).getId());
                    return dir == Sort.Direction.DESC ?
                            scoreB.compareTo(scoreA) : scoreA.compareTo(scoreB);
                });
            } else {
                // Default sorting
                sortedGroups.sort((a, b) -> {
                    LocalDateTime dateA = getMaxDate(a.getValue());
                    LocalDateTime dateB = getMaxDate(b.getValue());
                    return dir == Sort.Direction.DESC ?
                            dateB.compareTo(dateA) : dateA.compareTo(dateB);
                });
            }

            // Apply pagination
            int total = sortedGroups.size();
            int totalPages = (int) Math.ceil((double) total / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<ApplicantSummaryDTO> applicantSummaries = new ArrayList<>();

            if (fromIndex < total) {
                List<Map.Entry<String, List<JobApplication>>> pagedGroups = sortedGroups.subList(fromIndex, toIndex);

                for (Map.Entry<String, List<JobApplication>> entry : pagedGroups) {
                    String groupId = entry.getKey();
                    List<JobApplication> apps = entry.getValue();
                    JobApplication primaryApp = apps.get(0);

                    // Get AI Rating using either application ID or group ID
                    Optional<AIRating> aiRatingOpt;
                    if (groupId.startsWith("single_")) {
                        aiRatingOpt = aiRatingRepository.findByJobApplicationId(primaryApp.getId());
                    } else {
                        // First try to find by application group ID directly
                        aiRatingOpt = aiRatingRepository.findFirstByApplicationGroupId(groupId);

                        // If not found, fallback to first application ID
                        if (aiRatingOpt.isEmpty()) {
                            aiRatingOpt = aiRatingRepository.findByJobApplicationId(primaryApp.getId());
                        }
                    }

                    // Extract AI scores
                    BigDecimal finalScore = aiRatingOpt
                            .map(AIRating::getFinalScore)
                            .orElse(BigDecimal.ZERO);

                    BigDecimal experienceScore = aiRatingOpt
                            .map(AIRating::getExperienceScore)
                            .orElse(null);

                    BigDecimal skillsScore = aiRatingOpt
                            .map(AIRating::getSkillsScore)
                            .orElse(null);

                    BigDecimal locationScore = aiRatingOpt
                            .map(AIRating::getLocationScore)
                            .orElse(null);

                    BigDecimal availabilityScore = aiRatingOpt
                            .map(AIRating::getAvailabilityScore)
                            .orElse(null);

                    BigDecimal resumeScore = aiRatingOpt
                            .map(AIRating::getResumeScore)
                            .orElse(null);

                    BigDecimal reputationScore = aiRatingOpt
                            .map(AIRating::getReputationScore)
                            .orElse(null);

                    String aiFeedback = aiRatingOpt
                            .map(AIRating::getAiFeedback)
                            .orElse(null);

                    // Create applicant summary
                    ApplicantSummaryDTO summary = ApplicantSummaryDTO.builder()
                            .id(primaryApp.getId())
                            .candidateId(primaryApp.getCandidate().getId())
                            .candidateName(primaryApp.getCandidate().getName())
                            .email(primaryApp.getCandidate().getEmail())
                            .phoneNumber(primaryApp.getCandidate().getPhoneNumber())
                            .profilePictureUrl(primaryApp.getCandidate().getProfilePictureUrl())
                            .gender(primaryApp.getCandidate().getGender().toString())
                            .applicationStatus(primaryApp.getApplicationStatus().toString())
                            .applicationDate(primaryApp.getApplicationDate())
                            .locationNames(new ArrayList<>(flattenedLocationDatesMap.keySet()))
                            .locationWorkDates(flattenedLocationDatesMap)
                            .finalScore(finalScore)
                            .experienceScore(experienceScore)
                            .skillsScore(skillsScore)
                            .locationScore(locationScore)
                            .availabilityScore(availabilityScore)
                            .resumeScore(resumeScore)
                            .reputationScore(reputationScore)
                            .aiFeedback(aiFeedback)
                            .distanceToJob(primaryApp.getDistanceToCandidate())
                            .applicationGroupId(groupId.startsWith("single_") ? null : groupId)
                            .notes(primaryApp.getNotes())
                            .withdrawalReason(primaryApp.getWithdrawalReason())
                            .build();

                    applicantSummaries.add(summary);
                }
            }

            // Create response with pagination info
            ApplicantsResponseDTO responseDTO = new ApplicantsResponseDTO(
                    applicantSummaries,
                    (long) total,
                    page,
                    size,
                    totalPages
            );

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved job applicants successfully", responseDTO);

        } catch (Exception e) {
            log.error("Error retrieving job applicants: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving job applicants: " + e.getMessage(), null);
        }
    }

    /**
     * Update the status of all applications in a group
     * @param groupId The application group ID
     * @param status The new status
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> updateApplicationGroupStatus(String groupId, String status) {
        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Find all applications with this group ID with complete details
            List<JobApplication> applications = jobApplicationRepository.findByApplicationGroupIdWithFullDetails(groupId);

            if (applications.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "No applications found for this group", null);
            }

            // Get the job and check recruiter permission
            JobApplication firstApp = applications.get(0);
            Jobs job = firstApp.getJobLocation().getJob();
            Projects project = job.getProject();
            Recruiters jobRecruiter = project.getRecruiter();

            // Security check - ensure the job belongs to the current recruiter
            if (!jobRecruiter.getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to update these applications", null);
            }

            // Validate status
            JobApplication.ApplicationStatus newStatus;
            try {
                newStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid application status: " + status, null);
            }

            // Update all applications in the group
            for (JobApplication application : applications) {
                // Update status
                application.setApplicationStatus(newStatus);

                // If hiring, set hired date and update position count
                if (newStatus == JobApplication.ApplicationStatus.HIRED) {
                    JobApplication.ApplicationStatus oldStatus = application.getApplicationStatus();
                    application.setHiredDate(LocalDateTime.now());

                    // Only increment positions filled if not already hired
                    if (oldStatus != JobApplication.ApplicationStatus.HIRED) {
                        application.getJobLocation().setPositionsFilled(
                                application.getJobLocation().getPositionsFilled() + 1);
                    }
                }

                // Save application
                jobApplicationRepository.save(application);
            }

            return new Response<>(HttpStatus.OK.value(),
                    "All applications in the group updated successfully", null);

        } catch (Exception e) {
            log.error("Error updating application group status: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error updating application group status: " + e.getMessage(), null);
        }
    }

    /**
     * Get the total number of applicants for a job
     * @param jobId The job ID
     * @return Response with the total count
     */
    public Response<Long> getJobApplicantsCount(Long jobId) {
        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Validate job exists and belongs to the recruiter
            Optional<Jobs> jobOpt = jobRepository.findByIdWithAllDetails(jobId);
            if (jobOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "Job not found", null);
            }

            Jobs job = jobOpt.get();
            Projects project = job.getProject();
            Recruiters jobRecruiter = project.getRecruiter();

            // Security check - ensure the job belongs to the current recruiter
            if (!jobRecruiter.getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view applicants for this job", null);
            }

            // Count unique candidates who applied to this job
            Long count = jobApplicationRepository.countDistinctCandidatesByJobId(jobId);

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved applicant count successfully", count);

        } catch (Exception e) {
            log.error("Error retrieving job applicants count: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving job applicants count: " + e.getMessage(), null);
        }
    }

    /**
     * Get job applicants statistics
     * @param jobId The job ID
     * @return Response with counts for different application statuses
     */
    public Response<Map<String, Long>> getJobApplicantsStats(Long jobId) {
        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Validate job exists and belongs to the recruiter
            Optional<Jobs> jobOpt = jobRepository.findByIdWithAllDetails(jobId);
            if (jobOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "Job not found", null);
            }

            Jobs job = jobOpt.get();
            Projects project = job.getProject();
            Recruiters jobRecruiter = project.getRecruiter();

            // Security check - ensure the job belongs to the current recruiter
            if (!jobRecruiter.getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view statistics for this job", null);
            }

            // Create map to store counts for each status
            Map<String, Long> stats = new HashMap<>();

            // Get total applicants
            Long totalApplicants = jobApplicationRepository.countDistinctCandidatesByJobId(jobId);
            stats.put("TOTAL", totalApplicants);

            // Get counts for each status
            for (JobApplication.ApplicationStatus status : JobApplication.ApplicationStatus.values()) {
                Long count = jobApplicationRepository.countDistinctCandidatesByJobIdAndStatus(jobId, status);
                stats.put(status.name(), count);
            }

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved applicant statistics successfully", stats);

        } catch (Exception e) {
            log.error("Error retrieving job applicants statistics: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving job applicants statistics: " + e.getMessage(), null);
        }
    }

    /**
     * Get applicants for a job filtered by status
     * @param jobId The job ID
     * @param status The status to filter by (or null for all)
     * @param page Page number
     * @param size Page size
     * @param sortBy Field to sort by
     * @param sortDir Sort direction
     * @return Response with filtered applicants
     */
    public Response<ApplicantsResponseDTO> getJobApplicantsByStatus(
            Long jobId,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Validate job exists and belongs to the recruiter
            Optional<Jobs> jobOpt = jobRepository.findByIdWithAllDetails(jobId);
            if (jobOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "Job not found", null);
            }

            Jobs job = jobOpt.get();
            Projects project = job.getProject();
            Recruiters jobRecruiter = project.getRecruiter();

            // Security check - ensure the job belongs to the current recruiter
            if (!jobRecruiter.getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view applicants for this job", null);
            }

            // Check if status is valid
            JobApplication.ApplicationStatus applicationStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    applicationStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return new Response<>(HttpStatus.BAD_REQUEST.value(),
                            "Invalid application status: " + status, null);
                }
            }

            // Get all applications for this job with complete details
            List<JobApplication> allApplications;

            if (applicationStatus != null) {
                allApplications = jobApplicationRepository.findByJobIdAndStatusWithFullDetails(jobId, applicationStatus);
            } else {
                allApplications = jobApplicationRepository.findByJobIdWithFullDetails(jobId);
            }

            if (allApplications.isEmpty()) {
                return new Response<>(HttpStatus.OK.value(),
                        "No applicants found for this job with status: " + status,
                        new ApplicantsResponseDTO(
                                new ArrayList<>(),
                                0L,
                                page,
                                size,
                                0));
            }

            // Group applications by applicationGroupId or by candidate if no group ID
            Map<String, List<JobApplication>> groupedApplications = new HashMap<>();

            for (JobApplication app : allApplications) {
                String groupKey = app.getApplicationGroupId() != null && !app.getApplicationGroupId().isEmpty()
                        ? app.getApplicationGroupId()
                        : "single_" + app.getCandidate().getId();

                if (!groupedApplications.containsKey(groupKey)) {
                    groupedApplications.put(groupKey, new ArrayList<>());
                }
                groupedApplications.get(groupKey).add(app);
            }

            // Collect unique location names with dates for each group
            Map<String, Set<LocalDateTime>> locationDatesMap = new LinkedHashMap<>();

            for (List<JobApplication> apps : groupedApplications.values()) {
                for (JobApplication app : apps) {
                    JobLocation jobLocation = app.getJobLocation();
                    String locationName = jobLocation.getLocation().getName();

                    // Get work date from schedule date if available
                    LocalDateTime workDate = getWorkDateForJobLocation(jobLocation);

                    // Use computeIfAbsent to create a new Set if the location doesn't exist
                    locationDatesMap.computeIfAbsent(locationName, k -> new LinkedHashSet<>()).add(workDate);
                }
            }

            // Convert Set of dates to a map with sorted dates for each location
            Map<String, List<LocalDateTime>> flattenedLocationDatesMap = locationDatesMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .sorted()
                                    .collect(Collectors.toList())
                    ));

            // Sort groups by the criteria
            List<Map.Entry<String, List<JobApplication>>> sortedGroups = new ArrayList<>(groupedApplications.entrySet());

            // Sort by the specified criteria
            final Sort.Direction dir = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;

            if (sortBy.equalsIgnoreCase("applicationDate")) {
                sortedGroups.sort((a, b) -> {
                    LocalDateTime dateA = getMaxDate(a.getValue());
                    LocalDateTime dateB = getMaxDate(b.getValue());
                    return dir == Sort.Direction.DESC ?
                            dateB.compareTo(dateA) : dateA.compareTo(dateB);
                });
            } else if (sortBy.equalsIgnoreCase("finalScore")) {
                sortedGroups.sort((a, b) -> {
                    BigDecimal scoreA = getGroupScore(a.getKey(), a.getValue().get(0).getId());
                    BigDecimal scoreB = getGroupScore(b.getKey(), b.getValue().get(0).getId());
                    return dir == Sort.Direction.DESC ?
                            scoreB.compareTo(scoreA) : scoreA.compareTo(scoreB);
                });
            } else {
                // Default sorting
                sortedGroups.sort((a, b) -> {
                    LocalDateTime dateA = getMaxDate(a.getValue());
                    LocalDateTime dateB = getMaxDate(b.getValue());
                    return dir == Sort.Direction.DESC ?
                            dateB.compareTo(dateA) : dateA.compareTo(dateB);
                });
            }

            // Apply pagination
            int total = sortedGroups.size();
            int totalPages = (int) Math.ceil((double) total / size);
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<ApplicantSummaryDTO> applicantSummaries = new ArrayList<>();

            if (fromIndex < total) {
                List<Map.Entry<String, List<JobApplication>>> pagedGroups = sortedGroups.subList(fromIndex, toIndex);

                for (Map.Entry<String, List<JobApplication>> entry : pagedGroups) {
                    String groupId = entry.getKey();
                    List<JobApplication> apps = entry.getValue();
                    JobApplication primaryApp = apps.get(0);

                    // Get AI Rating using either application ID or group ID
                    Optional<AIRating> aiRatingOpt;
                    if (groupId.startsWith("single_")) {
                        aiRatingOpt = aiRatingRepository.findByJobApplicationId(primaryApp.getId());
                    } else {
                        // First try to find by application group ID directly
                        aiRatingOpt = aiRatingRepository.findFirstByApplicationGroupId(groupId);

                        // If not found, fallback to first application ID
                        if (aiRatingOpt.isEmpty()) {
                            aiRatingOpt = aiRatingRepository.findByJobApplicationId(primaryApp.getId());
                        }
                    }

                    // Extract AI scores
                    BigDecimal finalScore = aiRatingOpt
                            .map(AIRating::getFinalScore)
                            .orElse(BigDecimal.ZERO);

                    BigDecimal experienceScore = aiRatingOpt
                            .map(AIRating::getExperienceScore)
                            .orElse(null);

                    BigDecimal skillsScore = aiRatingOpt
                            .map(AIRating::getSkillsScore)
                            .orElse(null);

                    BigDecimal locationScore = aiRatingOpt
                            .map(AIRating::getLocationScore)
                            .orElse(null);

                    BigDecimal availabilityScore = aiRatingOpt
                            .map(AIRating::getAvailabilityScore)
                            .orElse(null);

                    BigDecimal resumeScore = aiRatingOpt
                            .map(AIRating::getResumeScore)
                            .orElse(null);

                    BigDecimal reputationScore = aiRatingOpt
                            .map(AIRating::getReputationScore)
                            .orElse(null);

                    String aiFeedback = aiRatingOpt
                            .map(AIRating::getAiFeedback)
                            .orElse(null);

                    // Create applicant summary
                    ApplicantSummaryDTO summary = ApplicantSummaryDTO.builder()
                            .id(primaryApp.getId())
                            .candidateId(primaryApp.getCandidate().getId())
                            .candidateName(primaryApp.getCandidate().getName())
                            .email(primaryApp.getCandidate().getEmail())
                            .phoneNumber(primaryApp.getCandidate().getPhoneNumber())
                            .profilePictureUrl(primaryApp.getCandidate().getProfilePictureUrl())
                            .gender(primaryApp.getCandidate().getGender().toString())
                            .applicationStatus(primaryApp.getApplicationStatus().toString())
                            .applicationDate(primaryApp.getApplicationDate())
                            .locationNames(new ArrayList<>(flattenedLocationDatesMap.keySet()))
                            .locationWorkDates(flattenedLocationDatesMap)
                            .finalScore(finalScore)
                            .experienceScore(experienceScore)
                            .skillsScore(skillsScore)
                            .locationScore(locationScore)
                            .availabilityScore(availabilityScore)
                            .resumeScore(resumeScore)
                            .reputationScore(reputationScore)
                            .aiFeedback(aiFeedback)
                            .distanceToJob(primaryApp.getDistanceToCandidate())
                            .applicationGroupId(groupId.startsWith("single_") ? null : groupId)
                            .withdrawalReason(primaryApp.getWithdrawalReason())
                            .build();

                    applicantSummaries.add(summary);
                }
            }

            // Create response with pagination info
            ApplicantsResponseDTO responseDTO = new ApplicantsResponseDTO(
                    applicantSummaries,
                    (long) total,
                    page,
                    size,
                    totalPages
            );

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved job applicants successfully", responseDTO);

        } catch (Exception e) {
            log.error("Error retrieving job applicants by status: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving job applicants by status: " + e.getMessage(), null);
        }
    }

    /**
     * Helper method to get the maximum application date from a list of applications
     */
    private LocalDateTime getMaxDate(List<JobApplication> applications) {
        return applications.stream()
                .map(JobApplication::getApplicationDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    /**
     * Helper method to get AI score for a group
     */
    private BigDecimal getGroupScore(String groupId, Long defaultAppId) {
        if (groupId.startsWith("single_")) {
            // Single application
            return aiRatingRepository.findByJobApplicationId(defaultAppId)
                    .map(AIRating::getFinalScore)
                    .orElse(BigDecimal.ZERO);
        } else {
            // Group application - try to find by group ID first
            Optional<AIRating> rating = aiRatingRepository.findFirstByApplicationGroupId(groupId);
            if (rating.isPresent()) {
                return rating.get().getFinalScore();
            } else {
                // Fallback to the application ID
                return aiRatingRepository.findByJobApplicationId(defaultAppId)
                        .map(AIRating::getFinalScore)
                        .orElse(BigDecimal.ZERO);
            }
        }
    }

    /**
     * Helper method to get the work date for a job location with improved multi-date handling
     * @param jobLocation The job location
     * @return LocalDateTime representing the work date, or application date as fallback
     */
    private LocalDateTime getWorkDateForJobLocation(JobLocation jobLocation) {
        // Prioritize job location's specific schedule date
        if (jobLocation.getJobScheduleDate() != null) {
            // Convert the work date to LocalDateTime (midnight)
            return jobLocation.getJobScheduleDate().getWorkDate().atStartOfDay();
        }

        // Check job schedules if no specific schedule date
        if (jobLocation.getJob().getJobSchedules() != null && !jobLocation.getJob().getJobSchedules().isEmpty()) {
            // Get first schedule's start date and time
            var schedule = jobLocation.getJob().getJobSchedules().iterator().next();
            if (schedule.getStartDate() != null) {
                return schedule.getStartDate().atTime(schedule.getStartTime());
            }
        }

        // Fall back to current date if no dates are available
        return LocalDateTime.now();
    }

}
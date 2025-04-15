package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.JobApplicationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.WithdrawApplicationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.JobApplicationResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobScheduleDateRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.ai.AIRatingService;
import com.event.recruitment.intelligent_recruitment_system.service.email.EmailService;
import com.event.recruitment.intelligent_recruitment_system.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobLocationRepository jobLocationRepository;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;
    private final LocationService locationService;
    private final AIRatingService aiRatingService; // Added AIRatingService
    private JobSummaryResponseDTO jobSummary;

    @Transactional
    public Response<JobApplicationResponseDTO> applyForJob(JobApplicationRequest request) {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            if (request.getJobLocationIds() == null || request.getJobLocationIds().isEmpty()) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "At least one job location is required", null);
            }

            // Get all selected job locations
            List<JobLocation> jobLocations = new ArrayList<>();
            for (Long locationId : request.getJobLocationIds()) {
                Optional<JobLocation> jobLocationOpt = jobLocationRepository.findById(locationId);
                if (jobLocationOpt.isEmpty()) {
                    return new Response<>(HttpStatus.NOT_FOUND.value(),
                            "Job location not found with ID: " + locationId, null);
                }

                JobLocation jobLocation = jobLocationOpt.get();

                // Check if the position is still open
                if (jobLocation.getStatus() != JobLocationStatus.OPEN &&
                        jobLocation.getStatus() != JobLocationStatus.PARTIAL_FILLED) {
                    return new Response<>(HttpStatus.BAD_REQUEST.value(),
                            "Location " + jobLocation.getLocation().getName() + " is no longer accepting applications", null);
                }

                // Check if all positions are filled
                if (jobLocation.getPositionsFilled() >= jobLocation.getPositionsNeeded()) {
                    return new Response<>(HttpStatus.BAD_REQUEST.value(),
                            "All positions for " + jobLocation.getLocation().getName() + " have been filled", null);
                }

                // Check if candidate already applied for this job location
                boolean alreadyApplied = jobApplicationRepository.existsByJobLocationIdAndCandidateId(
                        jobLocation.getId(), candidate.getId());

                if (alreadyApplied) {
                    return new Response<>(HttpStatus.CONFLICT.value(),
                            "You have already applied for this position at " + jobLocation.getLocation().getName(), null);
                }

                jobLocations.add(jobLocation);
            }

            String applicationGroupId = JobApplication.generateGroupId();

            // First save applications without distance calculation
            List<JobApplication> savedApplications = new ArrayList<>();
            for (JobLocation jobLocation : jobLocations) {
                JobApplication application = JobApplication.builder()
                        .jobLocation(jobLocation)
                        .candidate(candidate)
                        .applicationStatus(JobApplication.ApplicationStatus.PENDING)
                        .notes(request.getNotes())
                        .applicationGroupId(applicationGroupId) // Set the same group ID
                        .build();

                savedApplications.add(jobApplicationRepository.save(application));
            }

            // Send email first, using the original transaction's database state
            try {
                sendApplicationConfirmationEmail(candidate, savedApplications);
            } catch (Exception e) {
                log.error("Error sending application confirmation email: {}", e.getMessage(), e);
                // Continue with the process even if email fails
            }

            // Convert to response DTO
            JobApplicationResponseDTO responseDTO = convertToGroupResponseDTO(savedApplications);

            // Store necessary IDs for the background task rather than entities
            final List<Long> applicationIds = savedApplications.stream()
                    .map(JobApplication::getId)
                    .collect(Collectors.toList());
            final Long candidateId = candidate.getId();
            // Store applicationGroupId for AI evaluation
            final String finalApplicationGroupId = applicationGroupId;

            // Schedule the distance calculation for later and chain the AI evaluation
            CompletableFuture.runAsync(() -> {
                try {
                    // First update distances in a new transaction
                    updateApplicationDistancesInNewTransaction(applicationIds, candidateId);

                    // After distance calculation is done, run AI evaluation in the background
                    // Wait a short time to ensure distance updates are committed
                    Thread.sleep(500);

                    // Run AI evaluation by group ID
                    try {
                        log.info("Starting AI evaluation for application group: {}", finalApplicationGroupId);
                        Response<?> evaluationResponse = aiRatingService.evaluateByGroupId(finalApplicationGroupId);

                        if (evaluationResponse.getStatusCode() != 200) {
                            log.warn("AI evaluation completed with non-success status: {} - {}",
                                    evaluationResponse.getStatusCode(), evaluationResponse.getMessage());
                        } else {
                            log.info("AI evaluation completed successfully for group: {}", finalApplicationGroupId);
                        }
                    } catch (Exception e) {
                        log.error("Error in AI evaluation for group {}: {}", finalApplicationGroupId, e.getMessage(), e);
                        // Don't rethrow - we want to keep this contained and not affect the user experience
                    }
                } catch (Exception e) {
                    log.error("Error in async distance calculation: {}", e.getMessage(), e);
                    // Even if distance calculation fails, try AI evaluation with whatever data we have
                    try {
                        aiRatingService.evaluateByGroupId(finalApplicationGroupId);
                    } catch (Exception ex) {
                        log.error("Error in fallback AI evaluation: {}", ex.getMessage(), ex);
                    }
                }
            });

            return new Response<>(HttpStatus.CREATED.value(),
                    "Job applications submitted successfully", responseDTO);

        } catch (Exception e) {
            log.error("Error applying for job: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error applying for job: " + e.getMessage(), null);
        }
    }

    /**
     * Update application distances in a new transaction to avoid conflicts
     * @param applicationIds IDs of applications to update
     * @param candidateId ID of the candidate
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateApplicationDistancesInNewTransaction(List<Long> applicationIds, Long candidateId) {
        try {
            // Fetch candidate with preferred location
            Optional<Candidates> candidateOpt = candidateRepository.findById(candidateId);
            if (candidateOpt.isEmpty() || candidateOpt.get().getPreferredLocation() == null) {
                return;
            }

            Candidates candidate = candidateOpt.get();

            // Process each application in a separate transaction
            for (Long applicationId : applicationIds) {
                updateSingleApplicationDistance(applicationId, candidate);
            }
        } catch (Exception e) {
            log.error("Error in updateApplicationDistancesInNewTransaction: {}", e.getMessage(), e);
        }
    }

    /**
     * Update a single application's distance in its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSingleApplicationDistance(Long applicationId, Candidates candidate) {
        try {
            // We need to load the entire object graph within this transaction
            // Using a join fetch query to load job location and location in one go
            JobApplication application = jobApplicationRepository.findApplicationWithLocationById(applicationId);

            if (application == null) {
                log.warn("Application with ID {} not found for distance update", applicationId);
                return;
            }

            // Get location from the fully loaded entities
            Location jobLocationEntity = application.getJobLocation().getLocation();

            // If location is still null, we can't proceed
            if (jobLocationEntity == null) {
                log.warn("Job location's location is null for application ID {}", applicationId);
                return;
            }

            // Calculate distance
            Double distance = null;
            if (candidate.getPreferredLocation() != null) {
                distance = locationService.calculateDistance(
                        candidate.getPreferredLocation().getLatitude().doubleValue(),
                        candidate.getPreferredLocation().getLongitude().doubleValue(),
                        jobLocationEntity.getLatitude().doubleValue(),
                        jobLocationEntity.getLongitude().doubleValue()
                );
            }

            // Update the application
            application.setDistanceToCandidate(distance);
            jobApplicationRepository.save(application);

            log.debug("Updated distance for application ID {}: {} km",
                    applicationId,
                    distance != null ? String.format("%.2f", distance) : "null");

        } catch (Exception e) {
            log.error("Error updating distance for application ID {}: {}",
                    applicationId, e.getMessage(), e);
        }
    }

    // Rest of the existing methods remain unchanged...

    // Existing methods from the original class...
    private Double calculateDistanceToCandidateLocation(Candidates candidate, Location jobLocation) {
        // If candidate doesn't have a preferred location, return null
        if (candidate.getPreferredLocation() == null) {
            return null;
        }

        try {
            // Get candidate's preferred location coordinates directly from the entity
            BigDecimal candidateLatitude = null;
            BigDecimal candidateLongitude = null;

            // Get coordinates from the preferred location entity
            if (candidate.getPreferredLocation() != null) {
                candidateLatitude = candidate.getPreferredLocation().getLatitude();
                candidateLongitude = candidate.getPreferredLocation().getLongitude();
            }

            // If we couldn't get coordinates, return null
            if (candidateLatitude == null || candidateLongitude == null) {
                return null;
            }

            // Calculate distance using Haversine formula
            return locationService.calculateDistance(
                    candidateLatitude.doubleValue(), candidateLongitude.doubleValue(),
                    jobLocation.getLatitude().doubleValue(), jobLocation.getLongitude().doubleValue()
            );
        } catch (Exception e) {
            log.error("Error calculating distance to candidate location: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send job application confirmation email to the candidate
     *
     * @param candidate the candidate who applied
     * @param applications the list of job applications
     */
    private void sendApplicationConfirmationEmail(Candidates candidate, List<JobApplication> applications) {
        try {
            if (applications == null || applications.isEmpty() || candidate.getEmail() == null) {
                log.warn("Unable to send confirmation email: missing data");
                return;
            }

            // Use the first application for most details
            JobApplication firstApp = applications.get(0);

            // Get the job title and company name
            String jobTitle = firstApp.getJobLocation().getJob().getTitle();
            String companyName = firstApp.getJobLocation().getJob().getProject().getRecruiter().getCompanyName();

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = LocalDateTime.now().format(formatter);

            // Get all location names
            List<String> locationNames = applications.stream()
                    .map(app -> app.getJobLocation().getLocation().getName())
                    .distinct() // Add this line to remove duplicates
                    .collect(Collectors.toList());

            // Get all work dates
            List<LocalDate> workDates = applications.stream()
                    .map(app -> app.getJobLocation().getJobScheduleDate().getWorkDate())
                    .sorted()
                    .collect(Collectors.toList());

            // Format work dates
            List<String> formattedWorkDates = workDates.stream()
                    .map(date -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .collect(Collectors.toList());

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("candidateName", candidate.getName());
            variables.put("jobTitle", jobTitle);
            variables.put("companyName", companyName);
            variables.put("locations", String.join(", ", locationNames));
            variables.put("applicationDate", formattedDate);
            variables.put("applicationStatus", "PENDING");
            variables.put("workDates", formattedWorkDates);
            variables.put("isMultiLocation", locationNames.size() > 1);
            variables.put("salary", firstApp.getJobLocation().getJob().getSalary());
            variables.put("salaryType", firstApp.getJobLocation().getJob().getSalaryType());

            // Add distance information if available
            if (firstApp.getDistanceToCandidate() != null) {
                String formattedDistance = String.format("%.1f", firstApp.getDistanceToCandidate());
                variables.put("distanceToCandidate", formattedDistance);
                variables.put("hasDistanceInfo", true);
            } else {
                variables.put("hasDistanceInfo", false);
            }

            // Send email
            boolean emailSent = emailService.sendTemplateEmail(
                    candidate.getEmail(),
                    "Job Application Confirmation - " + jobTitle,
                    "email/job-application-confirmation",
                    variables
            );

            if (emailSent) {
                log.info("Application confirmation email sent to: {}", candidate.getEmail());
            } else {
                log.warn("Failed to send application confirmation email to: {}", candidate.getEmail());
            }
        } catch (Exception e) {
            // Log error but don't interrupt the application process
            log.error("Error sending application confirmation email: {}", e.getMessage(), e);
        }
    }

    public Response<List<JobApplicationResponseDTO>> getCandidateApplications() {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Long candidateId = candidateOpt.get().getId();

            // Get all applications for this candidate
            List<JobApplication> allApplications =
                    jobApplicationRepository.findByCandidateIdOrderByApplicationDateDesc(candidateId);

            if (allApplications.isEmpty()) {
                return new Response<>(HttpStatus.OK.value(),
                        "No applications found", new ArrayList<>());
            }

            // Group applications by applicationGroupId
            Map<String, List<JobApplication>> groupedApplications = new HashMap<>();

            for (JobApplication app : allApplications) {
                String groupKey = app.getApplicationGroupId() != null ?
                        app.getApplicationGroupId() : "single-" + app.getId();

                groupedApplications.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(app);
            }

            // Convert each group to a response DTO
            List<JobApplicationResponseDTO> responseDTOs = new ArrayList<>();

            for (Map.Entry<String, List<JobApplication>> entry : groupedApplications.entrySet()) {
                List<JobApplication> applicationGroup = entry.getValue();

                // If it's a grouped application (has multiple applications with same group ID)
                if (applicationGroup.size() > 1 && applicationGroup.get(0).getApplicationGroupId() != null) {
                    responseDTOs.add(convertToGroupResponseDTO(applicationGroup));
                } else {
                    // Single application (no group ID or only one in the group)
                    JobApplication application = applicationGroup.get(0);
                    // Check if using work dates repository or converting single application
                    if (application.getApplicationGroupId() == null) {
                        responseDTOs.add(convertToResponseDTO(application));
                    } else {
                        // It has a group ID but is alone in its group
                        responseDTOs.add(convertToGroupResponseDTO(applicationGroup));
                    }
                }
            }

            // Sort by application date (most recent first)
            responseDTOs.sort((a, b) -> b.getApplicationDate().compareTo(a.getApplicationDate()));

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved candidate applications successfully", responseDTOs);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving applications: " + e.getMessage(), null);
        }
    }

    public Response<JobApplicationResponseDTO> getApplicationDetails(Long applicationId) {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Optional<JobApplication> applicationOpt =
                    jobApplicationRepository.findByIdWithJobDetails(applicationId);

            if (applicationOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Application not found", null);
            }

            JobApplication application = applicationOpt.get();

            // Security check - ensure the application belongs to the current candidate
            if (!application.getCandidate().getId().equals(candidateOpt.get().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view this application", null);
            }

            JobApplicationResponseDTO responseDTO = convertToResponseDTO(application);

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved application details successfully", responseDTO);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving application details: " + e.getMessage(), null);
        }
    }

    @Transactional
    public Response<?> withdrawApplication(Long applicationId, WithdrawApplicationRequest request) {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(applicationId);

            if (applicationOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Application not found", null);
            }

            JobApplication application = applicationOpt.get();

            // Security check - ensure the application belongs to the current candidate
            if (!application.getCandidate().getId().equals(candidateOpt.get().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to withdraw this application", null);
            }

            // Check if the application can be withdrawn (not already hired)
            if (application.getApplicationStatus() == JobApplication.ApplicationStatus.HIRED) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Cannot withdraw an application for a job you've been hired for", null);
            }

            // Update the application status and withdrawal reason
            application.setApplicationStatus(JobApplication.ApplicationStatus.WITHDRAWN);
            application.setWithdrawalReason(request.getWithdrawalReason());
            jobApplicationRepository.save(application);

            return new Response<>(HttpStatus.OK.value(),
                    "Application withdrawn successfully", null);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error withdrawing application: " + e.getMessage(), null);
        }
    }

    /**
     * Get applications by group ID
     * @param groupId The application group ID
     * @return Response with applications grouped by the group ID
     */
    public Response<JobApplicationResponseDTO> getApplicationsByGroup(String groupId) {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find all applications with this group ID
            List<JobApplication> applications = jobApplicationRepository.findByApplicationGroupId(groupId);

            if (applications.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "No applications found for this group", null);
            }

            // Security check - ensure the applications belong to the current candidate
            boolean hasAccess = applications.stream()
                    .allMatch(app -> app.getCandidate().getId().equals(candidate.getId()));

            if (!hasAccess) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to view these applications", null);
            }

            // Convert to grouped response DTO
            JobApplicationResponseDTO responseDTO = convertToGroupResponseDTO(applications);

            return new Response<>(HttpStatus.OK.value(),
                    "Retrieved application group successfully", responseDTO);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving application group: " + e.getMessage(), null);
        }
    }

    /**
     * Withdraw all applications in a group
     * @param groupId The application group ID
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> withdrawApplicationsByGroup(String groupId, WithdrawApplicationRequest request) {
        try {
            String username = securityUtil.getCurrentUsername();
            Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

            if (candidateOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Candidate not found", null);
            }

            Candidates candidate = candidateOpt.get();

            // Find all applications with this group ID
            List<JobApplication> applications = jobApplicationRepository
                    .findByApplicationGroupIdAndCandidateId(groupId, candidate.getId());

            if (applications.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "No applications found for this group", null);
            }

            // Check if any applications are in HIRED status
            boolean anyHired = applications.stream()
                    .anyMatch(app -> app.getApplicationStatus() == JobApplication.ApplicationStatus.HIRED);

            if (anyHired) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Cannot withdraw applications as one or more are in HIRED status", null);
            }

            // Update all applications to WITHDRAWN status with the withdrawal reason
            for (JobApplication application : applications) {
                application.setApplicationStatus(JobApplication.ApplicationStatus.WITHDRAWN);
                application.setWithdrawalReason(request.getWithdrawalReason());
                jobApplicationRepository.save(application);
            }

            return new Response<>(HttpStatus.OK.value(),
                    "All applications in the group withdrawn successfully", null);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error withdrawing applications: " + e.getMessage(), null);
        }
    }

    private JobApplicationResponseDTO convertToGroupResponseDTO(List<JobApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return null;
        }

        // Use the first application for most details
        JobApplication firstApp = applications.get(0);

        // Get all location names
        List<String> locationNames = applications.stream()
                .map(app -> app.getJobLocation().getLocation().getName())
                .collect(Collectors.toList());

        // Get all work dates
        List<LocalDate> workDates = applications.stream()
                .map(app -> app.getJobLocation().getJobScheduleDate().getWorkDate())
                .sorted()
                .collect(Collectors.toList());

        List<Long> applicationIds = applications.stream()
                .map(JobApplication::getId)
                .collect(Collectors.toList());

        // Create job summary using the first application
        JobSummaryResponseDTO jobSummary = convertToJobSummaryDTO(firstApp);

        // Calculate average distance across all applications in the group
        Double avgDistance = applications.stream()
                .map(JobApplication::getDistanceToCandidate)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return JobApplicationResponseDTO.builder()
                .id(firstApp.getId()) // Using first application ID as reference
                .jobId(firstApp.getJobLocation().getJob().getId())
                .applicationGroupId(firstApp.getApplicationGroupId())
                .jobTitle(firstApp.getJobLocation().getJob().getTitle())
                .companyName(firstApp.getJobLocation().getJob().getProject().getRecruiter().getCompanyName())
                .locationNames(locationNames)
                .applicationStatus(firstApp.getApplicationStatus().name())
                .applicationDate(firstApp.getApplicationDate())
                .notes(firstApp.getNotes())
                .workDates(workDates)
                .applicationIds(applicationIds)
                .jobSummary(jobSummary) // Add job summary
                .distanceToCandidate(avgDistance > 0 ? avgDistance : null) // Add the average distance
                .build();
    }

    private JobApplicationResponseDTO convertToResponseDTO(JobApplication application) {
        // Create job summary
        JobSummaryResponseDTO jobSummary = convertToJobSummaryDTO(application);

        return JobApplicationResponseDTO.builder()
                .id(application.getId())
                .jobId(application.getJobLocation().getJob().getId())
                .jobTitle(application.getJobLocation().getJob().getTitle())
                .companyName(application.getJobLocation().getJob().getProject().getRecruiter().getCompanyName())
                .locationNames(List.of(application.getJobLocation().getLocation().getName()))
                .applicationStatus(application.getApplicationStatus().name())
                .applicationDate(application.getApplicationDate())
                .notes(application.getNotes())
                .applicationIds(List.of(application.getId()))
                .jobSummary(jobSummary) // Add job summary
                .distanceToCandidate(application.getDistanceToCandidate()) // Add the distance
                .build();
    }

    private JobSummaryResponseDTO convertToJobSummaryDTO(JobApplication application) {
        // Extract job details
        var job = application.getJobLocation().getJob();
        var project = job.getProject();
        var recruiter = project.getRecruiter();

        // Get all locations for this job
        List<String> locations = jobLocationRepository.findByJobId(job.getId()).stream()
                .map(loc -> loc.getLocation().getName())
                .distinct()
                .collect(Collectors.toList());

        // Get schedule information
        var schedules = job.getJobSchedules();
        LocalDate earliestStartDate = schedules.stream()
                .map(s -> s.getStartDate())
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate latestEndDate = schedules.stream()
                .map(s -> s.getEndDate())
                .max(LocalDate::compareTo)
                .orElse(null);

        // Use the first schedule for times (assuming all schedules have same time)
        LocalTime startTime = schedules.isEmpty() ? null : schedules.get(0).getStartTime();
        LocalTime endTime = schedules.isEmpty() ? null : schedules.get(0).getEndTime();

        // Calculate positions
        Integer totalPositions = schedules.stream()
                .mapToInt(s -> s.getNumPositions())
                .sum();

        // Calculate available positions (total minus filled)
        Integer filledPositions = jobLocationRepository.findByJobId(job.getId()).stream()
                .mapToInt(loc -> loc.getPositionsFilled())
                .sum();

        Integer availablePositions = totalPositions - filledPositions;

        // Build and return the job summary DTO
        return JobSummaryResponseDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .recruiterId(recruiter.getId())
                .companyName(recruiter.getCompanyName())
                .companyLogoUrl(recruiter.getCompanyLogoUrl())
                .recruiterType(recruiter.getRecruiterType())
                .locations(locations)
                .salary(job.getSalary())
                .salaryType(job.getSalaryType())
                .paymentTerms(job.getPaymentTerms())
                .benefits(job.getBenefits())
                .jobRequirements(job.getRequirements())
                .jobScope(job.getJobScope())
                .createdAt(job.getCreatedAt())
                .earliestStartDate(earliestStartDate)
                .latestEndDate(latestEndDate)
                .startTime(startTime)
                .endTime(endTime)
                .totalPositions(totalPositions)
                .availablePositions(availablePositions)
                .saved(false) // Default value, can be updated later if needed
                .viewed(true) // The job is viewed since it's applied
                .build();
    }
}
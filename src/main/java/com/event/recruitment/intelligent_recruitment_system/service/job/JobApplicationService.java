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
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobScheduleDateRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.ai.AIRatingService;
import com.event.recruitment.intelligent_recruitment_system.service.candidate.CandidateReputationService;
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
    private final AIRatingService aiRatingService;
    private JobSummaryResponseDTO jobSummary;
    private final CandidateReputationService candidateReputationService;


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
            Set<LocalDate> newApplicationDates = new HashSet<>();

            for (Long locationId : request.getJobLocationIds()) {
                Optional<JobLocation> jobLocationOpt = jobLocationRepository.findById(locationId);
                if (jobLocationOpt.isEmpty()) {
                    return new Response<>(HttpStatus.NOT_FOUND.value(),
                            "Job location not found with ID: " + locationId, null);
                }

                JobLocation jobLocation = jobLocationOpt.get();

                // Add work date from this job location
                if (jobLocation.getJobScheduleDate() != null) {
                    newApplicationDates.add(jobLocation.getJobScheduleDate().getWorkDate());
                }

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

            // Check for conflicts with HIRED applications
            if (!newApplicationDates.isEmpty()) {
                // Find all HIRED applications for this candidate
                List<JobApplication> hiredApplications = jobApplicationRepository
                        .findByCandidateIdAndApplicationStatus(
                                candidate.getId(),
                                JobApplication.ApplicationStatus.HIRED);

                // Get all work dates from hired applications
                Set<LocalDate> hiredDates = new HashSet<>();
                for (JobApplication app : hiredApplications) {
                    if (app.getJobLocation() != null &&
                            app.getJobLocation().getJobScheduleDate() != null) {
                        hiredDates.add(app.getJobLocation().getJobScheduleDate().getWorkDate());
                    }
                }

                // Check for overlap between new dates and hired dates
                Set<LocalDate> conflictDates = new HashSet<>(newApplicationDates);
                conflictDates.retainAll(hiredDates); // Keep only dates that exist in both sets

                if (!conflictDates.isEmpty()) {
                    // Format the conflict dates for the error message
                    List<String> formattedConflictDates = conflictDates.stream()
                            .sorted()
                            .map(date -> date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                            .collect(Collectors.toList());

                    return new Response<>(HttpStatus.CONFLICT.value(),
                            "You already have hired applications for these dates: " +
                                    String.join(", ", formattedConflictDates), null);
                }
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

            // Send email notifications
            try {
                // Send email to candidate
                sendApplicationConfirmationEmail(candidate, savedApplications);

                // Send email to recruiter about the new application
                sendNewApplicationEmailToRecruiter(candidate, savedApplications);
            } catch (Exception e) {
                log.error("Error sending application confirmation emails: {}", e.getMessage(), e);
                // Continue with the process even if emails fail
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

            // Get the previous status for the notification
            JobApplication.ApplicationStatus oldStatus = application.getApplicationStatus();
            boolean wasHired = (oldStatus == JobApplication.ApplicationStatus.HIRED);

            // Check if the application is in HIRED status
            if (wasHired) {
                // Handle hired job withdrawal separately with reputation impact
                Response<?> withdrawResponse = withdrawFromHiredJob(application, request.getWithdrawalReason());

                // Send withdrawal notification to recruiter regardless of the withdrawal response
                try {
                    sendWithdrawalEmailToRecruiter(application, oldStatus, request.getWithdrawalReason());
                } catch (Exception e) {
                    log.error("Error sending withdrawal notification to recruiter: {}", e.getMessage(), e);
                    // Continue with the process even if email fails
                }

                return withdrawResponse;
            }

            // For non-hired applications, proceed as before
            application.setApplicationStatus(JobApplication.ApplicationStatus.WITHDRAWN);
            application.setWithdrawalReason(request.getWithdrawalReason());
            jobApplicationRepository.save(application);

            // Send withdrawal notification to recruiter
            try {
                sendWithdrawalEmailToRecruiter(application, oldStatus, request.getWithdrawalReason());
            } catch (Exception e) {
                log.error("Error sending withdrawal notification to recruiter: {}", e.getMessage(), e);
                // Continue with the process even if email fails
            }

            return new Response<>(HttpStatus.OK.value(),
                    "Application withdrawn successfully", null);

        } catch (Exception e) {
            log.error("Error withdrawing application: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error withdrawing application: " + e.getMessage(), null);
        }
    }

    /**
     * Withdraw all applications in a group
     *
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

            // Store the previous statuses of each application
            Map<Long, JobApplication.ApplicationStatus> previousStatuses = applications.stream()
                    .collect(Collectors.toMap(
                            JobApplication::getId,
                            JobApplication::getApplicationStatus
                    ));

            // Update all applications to WITHDRAWN status with the withdrawal reason
            for (JobApplication application : applications) {
                application.setApplicationStatus(JobApplication.ApplicationStatus.WITHDRAWN);
                application.setWithdrawalReason(request.getWithdrawalReason());
                jobApplicationRepository.save(application);

                // Update job location's filled positions count if it was HIRED
                if (previousStatuses.get(application.getId()) == JobApplication.ApplicationStatus.HIRED) {
                    JobLocation jobLocation = application.getJobLocation();
                    if (jobLocation != null) {
                        int currentFilled = jobLocation.getPositionsFilled();
                        jobLocation.setPositionsFilled(Math.max(0, currentFilled - 1));

                        // Update status based on positions filled
                        if (jobLocation.getPositionsFilled() == 0) {
                            jobLocation.setStatus(JobLocationStatus.OPEN);
                        } else if (jobLocation.getPositionsFilled() < jobLocation.getPositionsNeeded()) {
                            jobLocation.setStatus(JobLocationStatus.PARTIAL_FILLED);
                        }

                        jobLocationRepository.save(jobLocation);
                    }
                }
            }

            // If any were in HIRED status, apply a SINGLE reputation penalty for the group
            if (anyHired) {
                candidateReputationService.applyHiredWithdrawalPenaltyForGroup(
                        candidate.getId(),
                        groupId,
                        request.getWithdrawalReason()
                );
            }

            // Send withdrawal notification to recruiters
            try {
                // Use the first application to get recruiter info
                JobApplication firstApp = applications.get(0);
                sendGroupWithdrawalEmailToRecruiter(applications, previousStatuses, request.getWithdrawalReason());
            } catch (Exception e) {
                log.error("Error sending group withdrawal notification to recruiter: {}", e.getMessage(), e);
                // Continue with the process even if email fails
            }

            if (anyHired) {
                return new Response<>(HttpStatus.OK.value(),
                        "All applications in the group withdrawn successfully. A reputation penalty has been applied.", null);
            } else {
                return new Response<>(HttpStatus.OK.value(),
                        "All applications in the group withdrawn successfully", null);
            }

        } catch (Exception e) {
            log.error("Error withdrawing applications: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error withdrawing applications: " + e.getMessage(), null);
        }
    }

    /**
     * Send email to recruiter about a new application
     *
     * @param candidate    The candidate who applied
     * @param applications The list of job applications
     */
    private void sendNewApplicationEmailToRecruiter(Candidates candidate, List<JobApplication> applications) {
        try {
            if (applications == null || applications.isEmpty()) {
                log.warn("Unable to send new application email to recruiter: missing data");
                return;
            }

            // Use the first application to get recruiter info
            JobApplication firstApp = applications.get(0);
            Recruiters recruiter = firstApp.getJobLocation().getJob().getProject().getRecruiter();

            // Check if recruiter has an email
            if (recruiter.getEmail() == null || recruiter.getEmail().isEmpty()) {
                log.warn("Unable to send new application email: Recruiter email is missing");
                return;
            }

            // Get job details
            String jobTitle = firstApp.getJobLocation().getJob().getTitle();
            String companyName = recruiter.getCompanyName();

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = LocalDateTime.now().format(formatter);

            // Get all location names
            List<String> locationNames = applications.stream()
                    .map(app -> app.getJobLocation().getLocation().getName())
                    .distinct()
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
            variables.put("recruiterName", recruiter.getRecruiterRepName());
            variables.put("candidateName", candidate.getName());
            variables.put("candidateEmail", candidate.getEmail());
            variables.put("candidatePhone", candidate.getPhoneNumber());
            variables.put("candidateGender", candidate.getGender().toString());
            variables.put("jobTitle", jobTitle);
            variables.put("applicationDate", formattedDate);
            variables.put("locations", String.join(", ", locationNames));
            variables.put("isMultiLocation", locationNames.size() > 1);
            variables.put("workDates", formattedWorkDates);

            // Add candidate notes if available
            String notes = firstApp.getNotes();
            variables.put("hasCandidateNotes", notes != null && !notes.isEmpty());
            variables.put("candidateNotes", notes);

            // Add job stats information
            int totalPositions = firstApp.getJobLocation().getJob().getJobSchedules().stream()
                    .mapToInt(s -> s.getNumPositions())
                    .sum();

            int filledPositions = jobLocationRepository.findByJobId(firstApp.getJobLocation().getJob().getId()).stream()
                    .mapToInt(loc -> loc.getPositionsFilled())
                    .sum();

            int openPositions = totalPositions - filledPositions;

            // Count total and pending applications
            Long jobId = firstApp.getJobLocation().getJob().getId();
            Long totalApplications = jobApplicationRepository.countDistinctCandidatesByJobId(jobId);
            Long pendingApplications = jobApplicationRepository.countDistinctCandidatesByJobIdAndStatus(
                    jobId, JobApplication.ApplicationStatus.PENDING);

            variables.put("showStats", true);
            variables.put("totalApplications", totalApplications);
            variables.put("pendingApplications", pendingApplications);
            variables.put("openPositions", openPositions);

            // Add application URL
            String applicationUrl = "http://localhost:5173/recruiter/applications?jobId=" + jobId;
            variables.put("applicationUrl", applicationUrl);

            // We don't have AI scores yet at application time
            variables.put("hasAiScores", false);

            // Send email
            boolean emailSent = emailService.sendTemplateEmail(
                    recruiter.getEmail(),
                    "New Job Application Received - " + jobTitle,
                    "email/new-application-notification",
                    variables
            );

            if (emailSent) {
                log.info("New application notification email sent to recruiter: {}", recruiter.getEmail());
            } else {
                log.warn("Failed to send new application notification email to recruiter: {}", recruiter.getEmail());
            }
        } catch (Exception e) {
            // Log error but don't interrupt the application process
            log.error("Error sending new application notification to recruiter: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email to recruiter about a withdrawn application
     *
     * @param application      The job application being withdrawn
     * @param previousStatus   The status before withdrawal
     * @param withdrawalReason The reason for withdrawal
     */
    private void sendWithdrawalEmailToRecruiter(JobApplication application, JobApplication.ApplicationStatus previousStatus, String withdrawalReason) {
        try {
            if (application == null) {
                log.warn("Unable to send withdrawal email to recruiter: missing application data");
                return;
            }

            // Get recruiter info
            Recruiters recruiter = application.getJobLocation().getJob().getProject().getRecruiter();

            // Check if recruiter has an email
            if (recruiter.getEmail() == null || recruiter.getEmail().isEmpty()) {
                log.warn("Unable to send withdrawal email: Recruiter email is missing");
                return;
            }

            // Get candidate and job details
            Candidates candidate = application.getCandidate();
            String jobTitle = application.getJobLocation().getJob().getTitle();

            // Format dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String applicationDate = application.getApplicationDate().format(formatter);
            String withdrawalDate = LocalDateTime.now().format(formatter);

            // Check if the candidate was hired
            boolean wasHired = (previousStatus == JobApplication.ApplicationStatus.HIRED);

            // Get location name
            String locationName = application.getJobLocation().getLocation().getName();

            // Get work date
            List<String> formattedWorkDates = new ArrayList<>();
            if (application.getJobLocation().getJobScheduleDate() != null) {
                LocalDate workDate = application.getJobLocation().getJobScheduleDate().getWorkDate();
                formattedWorkDates.add(workDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            }

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("recruiterName", recruiter.getRecruiterRepName());
            variables.put("candidateName", candidate.getName());
            variables.put("candidateEmail", candidate.getEmail());
            variables.put("candidatePhone", candidate.getPhoneNumber());
            variables.put("jobTitle", jobTitle);
            variables.put("applicationDate", applicationDate);
            variables.put("withdrawalDate", withdrawalDate);
            variables.put("previousStatus", previousStatus.toString());
            variables.put("locations", locationName);
            variables.put("isMultiLocation", false);
            variables.put("workDates", formattedWorkDates);
            variables.put("withdrawalReason", withdrawalReason);
            variables.put("wasHired", wasHired);
            variables.put("reputationImpact", wasHired);

            // Set job management URL
            Long jobId = application.getJobLocation().getJob().getId();
            String jobManagementUrl = "http://localhost:5173/recruiter/applications?jobId=" + jobId;
            variables.put("jobManagementUrl", jobManagementUrl);

            // Send email
            boolean emailSent = emailService.sendTemplateEmail(
                    recruiter.getEmail(),
                    "Application Withdrawn - " + jobTitle,
                    "email/application-withdrawal-notification",
                    variables
            );

            if (emailSent) {
                log.info("Withdrawal notification email sent to recruiter: {}", recruiter.getEmail());
            } else {
                log.warn("Failed to send withdrawal notification email to recruiter: {}", recruiter.getEmail());
            }
        } catch (Exception e) {
            // Log error but don't interrupt the withdrawal process
            log.error("Error sending withdrawal notification to recruiter: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email to recruiter about a group of withdrawn applications
     *
     * @param applications     The job applications being withdrawn
     * @param previousStatuses Map of application IDs to their previous statuses
     * @param withdrawalReason The reason for withdrawal
     */
    private void sendGroupWithdrawalEmailToRecruiter(List<JobApplication> applications,
                                                     Map<Long, JobApplication.ApplicationStatus> previousStatuses,
                                                     String withdrawalReason) {
        try {
            if (applications == null || applications.isEmpty()) {
                log.warn("Unable to send group withdrawal email to recruiter: missing application data");
                return;
            }

            // Use the first application for basic info
            JobApplication firstApp = applications.get(0);

            // Get recruiter info
            Recruiters recruiter = firstApp.getJobLocation().getJob().getProject().getRecruiter();

            // Check if recruiter has an email
            if (recruiter.getEmail() == null || recruiter.getEmail().isEmpty()) {
                log.warn("Unable to send group withdrawal email: Recruiter email is missing");
                return;
            }

            // Get candidate and job details
            Candidates candidate = firstApp.getCandidate();
            String jobTitle = firstApp.getJobLocation().getJob().getTitle();

            // Format dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String applicationDate = firstApp.getApplicationDate().format(formatter);
            String withdrawalDate = LocalDateTime.now().format(formatter);

            // Check if any of the applications were in HIRED status
            boolean anyHired = previousStatuses.values().stream()
                    .anyMatch(status -> status == JobApplication.ApplicationStatus.HIRED);

            // Get all location names
            List<String> locationNames = applications.stream()
                    .map(app -> app.getJobLocation().getLocation().getName())
                    .distinct()
                    .collect(Collectors.toList());

            // Get all work dates
            List<String> formattedWorkDates = applications.stream()
                    .filter(app -> app.getJobLocation().getJobScheduleDate() != null)
                    .map(app -> app.getJobLocation().getJobScheduleDate().getWorkDate()
                            .format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .distinct()
                    .collect(Collectors.toList());

            // Determine the overall previous status to display
            // Prioritize HIRED > PENDING > others
            JobApplication.ApplicationStatus displayStatus = previousStatuses.values().stream()
                    .filter(status -> status == JobApplication.ApplicationStatus.HIRED)
                    .findFirst()
                    .orElse(previousStatuses.values().stream()
                            .filter(status -> status == JobApplication.ApplicationStatus.PENDING)
                            .findFirst()
                            .orElse(previousStatuses.values().iterator().next()));

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("recruiterName", recruiter.getRecruiterRepName());
            variables.put("candidateName", candidate.getName());
            variables.put("candidateEmail", candidate.getEmail());
            variables.put("candidatePhone", candidate.getPhoneNumber());
            variables.put("jobTitle", jobTitle);
            variables.put("applicationDate", applicationDate);
            variables.put("withdrawalDate", withdrawalDate);
            variables.put("previousStatus", displayStatus.toString());
            variables.put("locations", String.join(", ", locationNames));
            variables.put("isMultiLocation", locationNames.size() > 1);
            variables.put("workDates", formattedWorkDates);
            variables.put("withdrawalReason", withdrawalReason);
            variables.put("wasHired", anyHired);
            variables.put("reputationImpact", anyHired);

            // Set job management URL
            Long jobId = firstApp.getJobLocation().getJob().getId();
            String jobManagementUrl = "http://localhost:5173/recruiter/applications?jobId=" + jobId;
            variables.put("jobManagementUrl", jobManagementUrl);

            // Send email
            boolean emailSent = emailService.sendTemplateEmail(
                    recruiter.getEmail(),
                    "Multiple Applications Withdrawn - " + jobTitle,
                    "email/application-withdrawal-notification",
                    variables
            );

            if (emailSent) {
                log.info("Group withdrawal notification email sent to recruiter: {}", recruiter.getEmail());
            } else {
                log.warn("Failed to send group withdrawal notification email to recruiter: {}", recruiter.getEmail());
            }
        } catch (Exception e) {
            // Log error but don't interrupt the withdrawal process
            log.error("Error sending group withdrawal notification to recruiter: {}", e.getMessage(), e);
        }
    }

    // Keep the existing methods below...
    // This includes sendApplicationConfirmationEmail, updateApplicationDistancesInNewTransaction,
    // withdrawFromHiredJob, and all other existing methods in the class

    /**
     * Send job application confirmation email to the candidate
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
                    .distinct()
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
            log.error("Error sending application confirmation email: {}", e.getMessage(), e);
        }
    }

    /**
     * Update application distances in a new transaction to avoid conflicts
     *
     * @param applicationIds IDs of applications to update
     * @param candidateId    ID of the candidate
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

    @Transactional
    protected Response<?> withdrawFromHiredJob(JobApplication application, String withdrawalReason) {
        try {
            // Update the application status and withdrawal reason
            application.setApplicationStatus(JobApplication.ApplicationStatus.WITHDRAWN);
            application.setWithdrawalReason(withdrawalReason);
            jobApplicationRepository.save(application);

            // Check if this application is part of a group
            String groupId = application.getApplicationGroupId();

            // Apply the reputation penalty
            Long candidateId = application.getCandidate().getId();
            Long jobApplicationId = application.getId();
            String description = "Withdrew from hired job: " + withdrawalReason;

            // If it's part of a group, use the group penalty method
            Response<?> reputationResponse;
            if (groupId != null) {
                reputationResponse = candidateReputationService.applyHiredWithdrawalPenaltyForGroup(
                        candidateId, groupId, withdrawalReason);
            } else {
                reputationResponse = candidateReputationService.applyHiredWithdrawalPenalty(
                        candidateId, jobApplicationId, description);
            }

            if (reputationResponse.getStatusCode() != 200) {
                log.warn("Failed to apply reputation penalty: {}", reputationResponse.getMessage());
            }

            // Update job location's filled positions count
            JobLocation jobLocation = application.getJobLocation();
            if (jobLocation != null) {
                int currentFilled = jobLocation.getPositionsFilled();
                jobLocation.setPositionsFilled(Math.max(0, currentFilled - 1));

                // Update status based on positions filled
                if (jobLocation.getPositionsFilled() == 0) {
                    jobLocation.setStatus(JobLocationStatus.OPEN);
                } else if (jobLocation.getPositionsFilled() < jobLocation.getPositionsNeeded()) {
                    jobLocation.setStatus(JobLocationStatus.PARTIAL_FILLED);
                }

                jobLocationRepository.save(jobLocation);
            }

            return new Response<>(HttpStatus.OK.value(),
                    "Application withdrawn successfully. Note: A reputation penalty has been applied for withdrawing from a job you were hired for.", null);

        } catch (Exception e) {
            log.error("Error withdrawing from hired job: {}", e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error withdrawing from hired job: " + e.getMessage(), null);
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
                .id(firstApp.getId())
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
                .jobSummary(jobSummary)
                .distanceToCandidate(avgDistance > 0 ? avgDistance : null)
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
                .jobSummary(jobSummary)
                .distanceToCandidate(application.getDistanceToCandidate())
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
                .saved(false)
                .viewed(true)
                .build();
    }
}
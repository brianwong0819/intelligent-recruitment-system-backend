package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.service.email.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationStatusUpdateService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobLocationRepository jobLocationRepository;
    private final JobRepository jobRepository;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;

    /**
     * Update the status of a job application with comprehensive checks and updates.
     * Includes validation against same-status updates and allowed transitions.
     * @param applicationId The ID of the job application
     * @param status The new status to set (e.g., "HIRED", "REJECTED", "PENDING")
     * @return Response indicating the result of the status update
     */
    @Transactional
    public Response<?> updateApplicationStatus(Long applicationId, String status) {
        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Find the application with full details
            Optional<JobApplication> applicationOpt = jobApplicationRepository.findByIdWithFullDetails(applicationId);
            if (applicationOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "Application not found", null);
            }

            JobApplication application = applicationOpt.get();

            // Security check - ensure the job belongs to the current recruiter
            if (!application.getJobLocation().getJob().getProject().getRecruiter().getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to update this application", null);
            }

            // Validate status string and convert to Enum
            JobApplication.ApplicationStatus newStatus;
            try {
                newStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid application status value: " + status, null);
            }

            // Get the job location and old status
            JobLocation jobLocation = application.getJobLocation();
            JobApplication.ApplicationStatus oldStatus = application.getApplicationStatus();

            // --- START Validation ---
            // 1. Check for same status
            if (oldStatus == newStatus) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Application status is already " + oldStatus, null);
            }

            // 2. Check for allowed transitions
            boolean transitionAllowed = false;
            switch (oldStatus) {
                case PENDING:
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.HIRED ||
                            newStatus == JobApplication.ApplicationStatus.REJECTED);
                    break;
                case HIRED:
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING ||
                            newStatus == JobApplication.ApplicationStatus.CANCELLED);
                    break;
                case REJECTED:
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.HIRED);
                    break;
                case CANCELLED:
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING);
                    break;
                default:
                    transitionAllowed = false;
                    break;
            }

            if (!transitionAllowed) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid status transition from " + oldStatus + " to " + newStatus, null);
            }
            // --- END Validation ---

            // Update application status in the object
            application.setApplicationStatus(newStatus);

            // Handle status-specific logic (calling helper methods)
            if (newStatus == JobApplication.ApplicationStatus.HIRED) {
                // handleHiredStatus now throws exception if location is full
                handleHiredStatus(jobLocation, application);
            } else if (oldStatus == JobApplication.ApplicationStatus.HIRED &&
                    (newStatus == JobApplication.ApplicationStatus.PENDING ||
                            newStatus == JobApplication.ApplicationStatus.CANCELLED)) {
                // Call handleUnhiredStatus only if the state changed FROM Hired TO a state that means not hired anymore
                handleUnhiredStatus(jobLocation, application);
            } else if (newStatus == JobApplication.ApplicationStatus.REJECTED || newStatus == JobApplication.ApplicationStatus.PENDING) {
                // Ensure hired date is null if moving to a non-hired state from a non-hired state
                if(oldStatus != JobApplication.ApplicationStatus.HIRED) {
                    application.setHiredDate(null);
                }
            }

            // Save the updated application
            jobApplicationRepository.save(application);

            // Check and update job status if needed
            checkAndUpdateJobStatus(jobLocation.getJob());

            // Check if this application is part of a group
            if (application.getApplicationGroupId() != null && !application.getApplicationGroupId().isEmpty()) {
                // For applications in a group, handle email at the group level to avoid duplicates
                // Get all applications in this group that have been updated to the same status
                List<JobApplication> groupApplications = jobApplicationRepository
                        .findByApplicationGroupIdAndApplicationStatus(
                                application.getApplicationGroupId(), newStatus);

                // Send a single email for the entire group
                if (!groupApplications.isEmpty()) {
                    sendGroupStatusUpdateEmail(application.getApplicationGroupId(), newStatus);
                }
            } else {
                // For individual applications (not part of a group), send individual email
                sendStatusUpdateEmail(application);
            }

            return new Response<>(HttpStatus.OK.value(),
                    "Application status updated successfully to " + newStatus, null);

        } catch (IllegalStateException ise) {
            // Catch specific exceptions like "Cannot hire - location positions are already filled"
            log.warn("State exception during application status update for ID {}: {}", applicationId, ise.getMessage());
            return new Response<>(HttpStatus.CONFLICT.value(),
                    ise.getMessage(), null);
        } catch (Exception e) {
            log.error("Error updating application status for ID {}: {}", applicationId, e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error updating application status: " + e.getMessage(), null);
        }
    }

    /**
     * Bulk update status for applications in a group.
     * Includes validation against same-status updates and allowed transitions per application.
     * Returns an informative message if no applications required an update.
     * @param groupId The application group ID
     * @param status The new status to set
     * @return Response indicating the result of the bulk status update
     */
    @Transactional // Ensures all-or-nothing update for the group
    public Response<?> updateApplicationGroupStatus(String groupId, String status) {
        try {
            // Get current recruiter's username
            String username = securityUtil.getCurrentUsername();

            // Find applications in the group with full details
            List<JobApplication> applications = jobApplicationRepository
                    .findByApplicationGroupIdWithFullDetails(groupId);

            if (applications.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(),
                        "No applications found for group ID: " + groupId, null);
            }

            // Security check - ensure the job belongs to the current recruiter (using first app)
            JobApplication firstApp = applications.get(0);
            if (!firstApp.getJobLocation().getJob().getProject().getRecruiter().getUsername().equals(username)) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You do not have permission to update applications in this group", null);
            }

            // Validate status string and convert to Enum
            JobApplication.ApplicationStatus newStatus;
            try {
                newStatus = JobApplication.ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid application status value: " + status, null);
            }

            boolean updateOccurred = false;

            // Process each application within the transaction
            for (JobApplication application : applications) {
                JobApplication.ApplicationStatus oldStatus = application.getApplicationStatus();
                JobLocation jobLocation = application.getJobLocation(); // Get location for this specific app

                // --- START Validation (within loop) ---
                // 1. Check for same status - skip this application if already correct
                if (oldStatus == newStatus) {
                    log.debug("Skipping application ID {}: Status is already {}", application.getId(), newStatus);
                    continue; // Move to the next application
                }

                // 2. Check for allowed transitions for THIS application
                boolean transitionAllowed = false;
                switch (oldStatus) {
                    case PENDING:
                        transitionAllowed = (newStatus == JobApplication.ApplicationStatus.HIRED ||
                                newStatus == JobApplication.ApplicationStatus.REJECTED);
                        break;
                    case HIRED:
                        transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING ||
                                newStatus == JobApplication.ApplicationStatus.CANCELLED); // Include CANCELLED if it means un-hiring
                        break;
                    case REJECTED:
                        transitionAllowed = (newStatus == JobApplication.ApplicationStatus.HIRED);
                        break;
                    case CANCELLED:
                        // Allow transition from CANCELLED to PENDING
                        transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING);
                        break;
                    // Add other cases as needed
                    default:
                        transitionAllowed = false;
                        break;
                }

                if (!transitionAllowed) {
                    log.error("Invalid status transition from {} to {} for application ID {} in group {}", oldStatus, newStatus, application.getId(), groupId);
                    throw new IllegalArgumentException("Invalid status transition from " + oldStatus + " to " + newStatus + " for application ID " + application.getId());
                }
                // --- END Validation ---

                // --- Mark that an update is happening ---
                updateOccurred = true; // Set flag because we passed validation and didn't 'continue'

                // Update status in the object
                application.setApplicationStatus(newStatus);

                // Handle status-specific logic (calling helper methods)
                if (newStatus == JobApplication.ApplicationStatus.HIRED) {
                    handleHiredStatus(jobLocation, application);
                } else if (oldStatus == JobApplication.ApplicationStatus.HIRED &&
                        (newStatus == JobApplication.ApplicationStatus.PENDING ||
                                newStatus == JobApplication.ApplicationStatus.CANCELLED)) {
                    handleUnhiredStatus(jobLocation, application);
                } else if (newStatus == JobApplication.ApplicationStatus.REJECTED || newStatus == JobApplication.ApplicationStatus.PENDING) {
                    if(oldStatus != JobApplication.ApplicationStatus.HIRED) {
                        application.setHiredDate(null);
                    }
                }

                // Save the updated application (still part of the transaction)
                jobApplicationRepository.save(application);
            } // End loop through applications

            if (!updateOccurred) {
                log.info("No applications required status update in group ID {} to status {}", groupId, newStatus);
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "All applications in the group are already in status " + newStatus, null);
            }

            // Update job statuses for all affected jobs (only if updates occurred)
            Set<Jobs> jobsToUpdate = applications.stream()
                    .filter(app -> app.getApplicationStatus() == newStatus) // Consider only apps that *might* have changed? Or all?
                    .map(app -> app.getJobLocation().getJob())
                    .collect(Collectors.toSet());

            for (Jobs job : jobsToUpdate) {
                checkAndUpdateJobStatus(job);
            }

            // Send a single email notification for the entire group
            if (updateOccurred) {
                sendGroupStatusUpdateEmail(groupId, newStatus);
            }

            // Return standard success message only if updates were actually made
            return new Response<>(HttpStatus.OK.value(),
                    "Application(s) in the group updated successfully to " + newStatus, null);

        } catch (IllegalStateException ise) {
            log.warn("State exception during group status update for group ID {}: {}", groupId, ise.getMessage());
            return new Response<>(HttpStatus.CONFLICT.value(),
                    ise.getMessage(), null);
        } catch (IllegalArgumentException iae) {
            log.error("Validation error during group status update for group ID {}: {}", groupId, iae.getMessage());
            return new Response<>(HttpStatus.BAD_REQUEST.value(),
                    iae.getMessage(), null);
        } catch (Exception e) {
            log.error("Error updating application group status for group ID {}: {}", groupId, e.getMessage(), e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error updating application group status: " + e.getMessage(), null);
        }
    }

    /**
     * Send a single email notification for all applications in a group
     * @param groupId The application group ID
     * @param status The new application status
     */
    private void sendGroupStatusUpdateEmail(String groupId, JobApplication.ApplicationStatus status) {
        try {
            // Get all applications in the group
            List<JobApplication> groupApplications = jobApplicationRepository.findByApplicationGroupId(groupId);

            if (groupApplications.isEmpty()) {
                log.warn("Cannot send group status update email: No applications found for group ID {}", groupId);
                return;
            }

            // Get the first application to get candidate information
            JobApplication firstApp = groupApplications.get(0);
            String candidateEmail = firstApp.getCandidate().getEmail();

            if (candidateEmail == null || candidateEmail.isEmpty()) {
                log.warn("Cannot send group status update email: Candidate email is missing for group ID {}", groupId);
                return;
            }

            // Get job information from the first application
            Jobs job = firstApp.getJobLocation().getJob();
            String jobTitle = job.getTitle();
            String companyName = job.getProject().getRecruiter().getCompanyName();

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String updateDate = LocalDateTime.now().format(formatter);

            // Extract all location names
            List<String> locationNames = groupApplications.stream()
                    .map(app -> app.getJobLocation().getLocation().getName())
                    .distinct()
                    .collect(Collectors.toList());

            // Extract all work dates
            List<String> formattedWorkDates = groupApplications.stream()
                    .map(app -> app.getJobLocation().getJobScheduleDate())
                    .filter(Objects::nonNull)
                    .map(date -> date.getWorkDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("candidateName", firstApp.getCandidate().getName());
            variables.put("jobTitle", jobTitle);
            variables.put("companyName", companyName);
            variables.put("applicationStatus", status.toString());
            variables.put("updateDate", updateDate);
            variables.put("locations", String.join(", ", locationNames));
            variables.put("isMultiLocation", locationNames.size() > 1);
            variables.put("workDates", formattedWorkDates);
            variables.put("totalDates", formattedWorkDates.size());

            // Include group ID as a reference
            variables.put("groupId", groupId);

            // Send the email
            boolean emailSent = emailService.sendTemplateEmail(
                    candidateEmail,
                    "Application Status Update: " + jobTitle,
                    "email/application-status-update",
                    variables
            );

            if (emailSent) {
                log.info("Group application status update email sent to: {} for group ID {}", candidateEmail, groupId);
            } else {
                log.warn("Failed to send group application status update email to: {} for group ID {}", candidateEmail, groupId);
            }
        } catch (Exception e) {
            log.error("Error sending group application status update email: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email notification to the candidate about their application status update
     * This is used only for non-grouped applications
     * @param application The job application with the updated status
     */
    private void sendStatusUpdateEmail(JobApplication application) {
        try {
            // Skip sending individual emails for applications that are part of a group
            // Those are handled by sendGroupStatusUpdateEmail
            if (application.getApplicationGroupId() != null && !application.getApplicationGroupId().isEmpty()) {
                log.debug("Skipping individual email for application ID {} as it's part of group {}",
                        application.getId(), application.getApplicationGroupId());
                return;
            }

            // Get candidate email
            String candidateEmail = application.getCandidate().getEmail();
            if (candidateEmail == null || candidateEmail.isEmpty()) {
                log.warn("Cannot send status update email: Candidate email is missing for application ID {}", application.getId());
                return;
            }

            Jobs job = application.getJobLocation().getJob();
            String jobTitle = job.getTitle();
            String companyName = job.getProject().getRecruiter().getCompanyName();
            JobApplication.ApplicationStatus newStatus = application.getApplicationStatus();

            // Format date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String updateDate = LocalDateTime.now().format(formatter);

            // Get location name
            String locationName = application.getJobLocation().getLocation().getName();

            // Get work date
            List<String> workDateList = new ArrayList<>();
            if (application.getJobLocation().getJobScheduleDate() != null) {
                LocalDate workDate = application.getJobLocation().getJobScheduleDate().getWorkDate();
                workDateList.add(workDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            }

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("candidateName", application.getCandidate().getName());
            variables.put("jobTitle", jobTitle);
            variables.put("companyName", companyName);
            variables.put("applicationStatus", newStatus.toString());
            variables.put("updateDate", updateDate);
            variables.put("locations", locationName);
            variables.put("isMultiLocation", false);
            variables.put("workDates", workDateList);
            variables.put("totalDates", workDateList.size());

            // Send the email
            boolean emailSent = emailService.sendTemplateEmail(
                    candidateEmail,
                    "Application Status Update: " + jobTitle,
                    "email/application-status-update",
                    variables
            );

            if (emailSent) {
                log.info("Application status update email sent to: {} for application ID {}", candidateEmail, application.getId());
            } else {
                log.warn("Failed to send application status update email to: {} for application ID {}", candidateEmail, application.getId());
            }
        } catch (Exception e) {
            // Log error but don't interrupt the status update process
            log.error("Error sending application status update email: {}", e.getMessage(), e);
        }
    }

    // --- Helper methods ---

    /**
     * Handle logic when an application is marked as HIRED.
     * Increments positions filled if possible, sets hired date, updates location status
     * accurately using OPEN, PARTIAL_FILLED, or FILLED.
     * @param jobLocation The job location associated with the application
     * @param application The job application being hired
     * @throws IllegalStateException if the location's positions are already filled.
     */
    private void handleHiredStatus(JobLocation jobLocation, JobApplication application) {
        // Check if the location still has available positions BEFORE incrementing
        if (jobLocation.getPositionsFilled() >= jobLocation.getPositionsNeeded()) {
            log.warn("Attempted to hire for application ID {} but location ID {} is already full (Filled: {}, Needed: {})",
                    application.getId(), jobLocation.getId(), jobLocation.getPositionsFilled(), jobLocation.getPositionsNeeded());
            throw new IllegalStateException("Cannot hire - location positions are already filled for location ID: " + jobLocation.getId());
        }

        // Increment positions filled only if it wasn't already considered hired
        if (application.getHiredDate() == null) {
            jobLocation.setPositionsFilled(jobLocation.getPositionsFilled() + 1);
        }

        // Set/update hired date
        application.setHiredDate(LocalDateTime.now());

        if (jobLocation.getStatus() != JobLocationStatus.CANCELLED) {
            int filled = jobLocation.getPositionsFilled();
            int needed = jobLocation.getPositionsNeeded();
            JobLocationStatus oldLocationStatus = jobLocation.getStatus();
            JobLocationStatus newLocationStatus;

            if (needed <= 0) { // Handle case where 0 positions are needed
                newLocationStatus = JobLocationStatus.FILLED; // Or perhaps CANCELLED/OPEN depending on business rule?
            } else if (filled >= needed) { // filled == needed is expected if check above passed
                newLocationStatus = JobLocationStatus.FILLED;
            } else if (filled > 0) {
                newLocationStatus = JobLocationStatus.PARTIAL_FILLED;
            } else { // filled is 0 (and needed > 0)
                newLocationStatus = JobLocationStatus.OPEN;
            }

            if (oldLocationStatus != newLocationStatus) {
                jobLocation.setStatus(newLocationStatus);
                log.info("Job location ID {} status updated from {} to {} based on counts (Filled: {}, Needed: {}).",
                        jobLocation.getId(), oldLocationStatus, newLocationStatus, filled, needed);
            }
        }

        // Save updated job location state
        jobLocationRepository.save(jobLocation);
    }

    /**
     * Handle logic when an application's status changes FROM HIRED.
     * Decrements positions filled, clears hired date, updates location status
     * accurately using OPEN, PARTIAL_FILLED, or FILLED.
     * @param jobLocation The job location associated with the application
     * @param application The job application being "un-hired"
     */
    private void handleUnhiredStatus(JobLocation jobLocation, JobApplication application) {
        // Only decrement if the application was actually hired (had a hired date)
        if (application.getHiredDate() != null) {
            log.info("Handling un-hiring for application ID {}. Decrementing positions filled for location ID {}.", application.getId(), jobLocation.getId());

            // Decrement positions filled, ensuring it doesn't go below zero
            jobLocation.setPositionsFilled(Math.max(0, jobLocation.getPositionsFilled() - 1));

            // Clear hired date
            application.setHiredDate(null);

            if (jobLocation.getStatus() != JobLocationStatus.CANCELLED) {
                int filled = jobLocation.getPositionsFilled();
                int needed = jobLocation.getPositionsNeeded();
                JobLocationStatus oldLocationStatus = jobLocation.getStatus();
                JobLocationStatus newLocationStatus;

                if (needed <= 0) { // Handle case where 0 positions are needed
                    newLocationStatus = JobLocationStatus.FILLED; // Or OPEN/CANCELLED?
                } else if (filled >= needed) { // Should not happen if we just decremented from a non-full state, but check anyway
                    newLocationStatus = JobLocationStatus.FILLED;
                } else if (filled > 0) { // filled > 0 and filled < needed
                    newLocationStatus = JobLocationStatus.PARTIAL_FILLED;
                } else { // filled is 0 (and needed > 0)
                    newLocationStatus = JobLocationStatus.OPEN;
                }

                if (oldLocationStatus != newLocationStatus) {
                    jobLocation.setStatus(newLocationStatus);
                    log.info("Job location ID {} status updated from {} to {} based on counts (Filled: {}, Needed: {}).",
                            jobLocation.getId(), oldLocationStatus, newLocationStatus, filled, needed);
                }
            }

            // Save updated job location state
            jobLocationRepository.save(jobLocation);
        } else {
            log.warn("handleUnhiredStatus called for application ID {} which did not have a hired date set.", application.getId());
        }
    }

    /**
     * Check and update the overall job status based on its associated job locations.
     * @param job The job to check and potentially update
     */
    private void checkAndUpdateJobStatus(Jobs job) {
        // Get current state of all job locations for this job
        List<JobLocation> jobLocations = jobLocationRepository.findByJobId(job.getId());

        // Filter out cancelled locations when determining job status based on hiring
        List<JobLocation> activeLocations = jobLocations.stream()
                .filter(loc -> loc.getStatus() != JobLocationStatus.CANCELLED)
                .collect(Collectors.toList());

        if (activeLocations.isEmpty()) {
            // If all locations are cancelled or there are no locations
            JobStatusType oldJobStatus = job.getStatus();
            // Consider setting to CLOSED if no active locations remain
            if (oldJobStatus != JobStatusType.CLOSED) {
                log.warn("Job ID {} has no active locations. Setting status to CLOSED.", job.getId());
                job.setStatus(JobStatusType.CLOSED);
                jobRepository.save(job);
            } else {
                log.info("Job ID {} has no active locations and is already CLOSED.", job.getId());
            }
            return; // No active locations to check counts against
        }

        int totalPositionsNeeded = activeLocations.stream().mapToInt(JobLocation::getPositionsNeeded).sum();
        int totalPositionsFilled = activeLocations.stream().mapToInt(JobLocation::getPositionsFilled).sum();

        JobStatusType oldJobStatus = job.getStatus();
        JobStatusType newJobStatus;

        // Determine the new status based on counts for active locations
        if (totalPositionsNeeded <= 0) {
            // No positions needed across all active locations
            log.info("Job ID {} has totalNeeded <= 0 across active locations. Setting status to FILLED/CLOSED.", job.getId());
            // Consider if FILLED or CLOSED is more appropriate when needed is 0
            newJobStatus = JobStatusType.CLOSED; // Or FILLED? Depends on business rule.
        } else if (totalPositionsFilled >= totalPositionsNeeded) {
            // All needed positions across active locations are filled
            newJobStatus = JobStatusType.FILLED;
        } else {
            // There are still open positions needed in active locations.
            newJobStatus = JobStatusType.OPEN;
        }

        // Only save if the status actually changes
        if (oldJobStatus != newJobStatus) {
            job.setStatus(newJobStatus);
            jobRepository.save(job);
            log.info("Job ID {} status updated from {} to {} based on active locations (Filled: {}, Needed: {})",
                    job.getId(), oldJobStatus, newJobStatus, totalPositionsFilled, totalPositionsNeeded);
        } else {
            log.info("Job ID {} status remains {}.", job.getId(), oldJobStatus);
        }
    }
}
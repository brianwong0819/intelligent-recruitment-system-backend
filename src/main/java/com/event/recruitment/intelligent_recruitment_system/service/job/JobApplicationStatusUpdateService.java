package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus; // Import the provided enum
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
// Assuming ApplicationStatus enum is defined within JobApplication like this:
// public class JobApplication {
//     public enum ApplicationStatus { PENDING, HIRED, REJECTED, CANCELLED /*, other statuses */ }
//     // ... other fields and methods
// }
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set; // Using Set for efficient lookup of distinct jobs/locations
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationStatusUpdateService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobLocationRepository jobLocationRepository;
    private final JobRepository jobRepository;
    private final SecurityUtil securityUtil;

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
                    // Define transitions allowed FROM Hired (e.g., back to Pending, maybe Cancelled)
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING ||
                            newStatus == JobApplication.ApplicationStatus.CANCELLED); // Adjust if needed
                    break;
                case REJECTED:
                    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.HIRED);
                    break;
                // Add cases for other statuses (e.g., CANCELLED) if transitions FROM them are allowed
                // case CANCELLED:
                //    transitionAllowed = (newStatus == JobApplication.ApplicationStatus.PENDING); // Example
                //    break;
                default:
                    // By default, transitions from unlisted/other statuses are not allowed
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
                            newStatus == JobApplication.ApplicationStatus.CANCELLED)) { // Check if CANCELLED should trigger un-hiring
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

            return new Response<>(HttpStatus.OK.value(),
                    "Application status updated successfully to " + newStatus, null);

        } catch (IllegalStateException ise) {
            // Catch specific exceptions like "Cannot hire - location positions are already filled"
            log.warn("State exception during application status update for ID {}: {}", applicationId, ise.getMessage());
            return new Response<>(HttpStatus.CONFLICT.value(), // 409 Conflict is suitable
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
     * @param groupId The application group ID
     * @param status The new status to set
     * @return Response indicating the result of the bulk status update
     */
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

            // --- ADDED: Flag to track if any update actually happened ---
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

            // --- ADDED: Check if any updates were made ---
            if (!updateOccurred) {
                log.info("No applications required status update in group ID {} to status {}", groupId, newStatus);
                // Return a success (200 OK) but with a different message,
                // or return a 400 Bad Request if you prefer to treat this as an "error" or invalid request.
                // Option 1: 200 OK with info message
                // return new Response<>(HttpStatus.OK.value(),
                //         "No applications required status update to " + newStatus, null);
                // Option 2: 400 Bad Request
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

        // --- Update job location status based on counts ---
        // Don't override CANCELLED status automatically by hiring counts
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
        // --- End status update ---

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

            // --- Update job location status based on counts ---
            // Don't override CANCELLED status automatically by un-hiring counts
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
            // --- End status update ---

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
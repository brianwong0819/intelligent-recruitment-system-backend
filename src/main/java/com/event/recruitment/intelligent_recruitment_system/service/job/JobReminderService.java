package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobReminderService {

    private final JobApplicationRepository jobApplicationRepository;
    private final EmailService emailService;

    /**
     * Scheduled task to send job reminders 48 hours before work starts.
     * Runs every 6 hours.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // Every 6 hours
    @Transactional(readOnly = true)
    public void sendUpcomingJobReminders() {
        log.info("Starting scheduled job to send 48-hour reminders");
        try {
            // Calculate the target date (48 hours from now)
            LocalDate targetDate = LocalDate.now().plusDays(2);
            log.info("Checking for jobs with work date: {}", targetDate);

            // Find all hired applications with work date 48 hours from now
            List<JobApplication> hiredApplications = jobApplicationRepository.findUpcomingHiredApplications(targetDate);

            if (hiredApplications.isEmpty()) {
                log.info("No upcoming jobs found for reminder emails");
                return;
            }

            log.info("Found {} hired applications with upcoming work date", hiredApplications.size());

            // Group applications by candidate and application group
            Map<String, List<JobApplication>> groupedApplications = new HashMap<>();

            for (JobApplication app : hiredApplications) {
                // Create a unique key using candidate ID and group ID (if available)
                String groupKey = app.getCandidate().getId() + "-" +
                        (app.getApplicationGroupId() != null ? app.getApplicationGroupId() : "single-" + app.getId());

                if (!groupedApplications.containsKey(groupKey)) {
                    groupedApplications.put(groupKey, new ArrayList<>());
                }

                groupedApplications.get(groupKey).add(app);
            }

            log.info("Grouped into {} unique candidate-job combinations", groupedApplications.size());

            // Process each group and send a single reminder email per candidate per job/group
            int emailsSent = 0;
            for (Map.Entry<String, List<JobApplication>> entry : groupedApplications.entrySet()) {
                List<JobApplication> applications = entry.getValue();
                if (sendJobReminderEmail(applications)) {
                    emailsSent++;
                }
            }

            log.info("Successfully sent {} reminder emails", emailsSent);

        } catch (Exception e) {
            log.error("Error sending job reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Send a job reminder email to a candidate for upcoming work
     *
     * @param applications List of applications for the same candidate and job/group
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendJobReminderEmail(List<JobApplication> applications) {
        if (applications == null || applications.isEmpty()) {
            return false;
        }

        try {
            // Use the first application to get candidate information
            JobApplication firstApp = applications.get(0);
            Candidates candidate = firstApp.getCandidate();

            // Skip if candidate has no email
            if (candidate.getEmail() == null || candidate.getEmail().isEmpty()) {
                log.warn("Cannot send reminder email: Candidate email is missing for ID {}", candidate.getId());
                return false;
            }

            // Get job information
            Jobs job = firstApp.getJobLocation().getJob();
            String jobTitle = job.getTitle();
            String companyName = job.getProject().getRecruiter().getCompanyName();

            // Get all location names
            List<String> locationNames = applications.stream()
                    .map(app -> app.getJobLocation().getLocation().getName())
                    .distinct()
                    .collect(Collectors.toList());

            // Get all work dates
            List<String> formattedWorkDates = applications.stream()
                    .filter(app -> app.getJobLocation().getJobScheduleDate() != null)
                    .map(app -> {
                        LocalDate workDate = app.getJobLocation().getJobScheduleDate().getWorkDate();
                        LocalTime startTime = app.getJobLocation().getJob().getJobSchedules().get(0).getStartTime();
                        LocalTime endTime = app.getJobLocation().getJob().getJobSchedules().get(0).getEndTime();

                        return workDate.format(DateTimeFormatter.ofPattern("E, dd MMM yyyy")) +
                                " (" + startTime.format(DateTimeFormatter.ofPattern("h:mm a")) +
                                " - " + endTime.format(DateTimeFormatter.ofPattern("h:mm a")) + ")";
                    })
                    .distinct()
                    .collect(Collectors.toList());

            // Prepare template variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("candidateName", candidate.getName());
            variables.put("jobTitle", jobTitle);
            variables.put("companyName", companyName);
            variables.put("locations", String.join(", ", locationNames));
            variables.put("isMultiLocation", locationNames.size() > 1);
            variables.put("workDates", formattedWorkDates);
            variables.put("reminderDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            // Add recruiter contact information
            String recruiterName = job.getProject().getRecruiter().getRecruiterRepName();
            String recruiterEmail = job.getProject().getRecruiter().getEmail();
            String recruiterPhone = job.getProject().getRecruiter().getPhoneNumber();

            variables.put("recruiterName", recruiterName);
            variables.put("recruiterEmail", recruiterEmail);
            variables.put("recruiterPhone", recruiterPhone);

            // Add job requirements and notes if available
            String requirements = job.getRequirements();
            if (requirements != null && !requirements.isEmpty()) {
                variables.put("hasRequirements", true);
                variables.put("requirements", requirements);
            } else {
                variables.put("hasRequirements", false);
            }

            // Send the email
            boolean emailSent = emailService.sendTemplateEmail(
                    candidate.getEmail(),
                    "REMINDER: Upcoming Job - " + jobTitle,
                    "email/job-reminder",
                    variables
            );

            if (emailSent) {
                log.info("Job reminder email sent to: {} for job: {}", candidate.getEmail(), jobTitle);
                return true;
            } else {
                log.warn("Failed to send job reminder email to: {} for job: {}", candidate.getEmail(), jobTitle);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending job reminder email: {}", e.getMessage(), e);
            return false;
        }
    }
}
// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/ai/CandidateAIEvaluationService.java
package com.event.recruitment.intelligent_recruitment_system.service.ai;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.ai.CandidateAIEvaluationDataDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateAvailabilityDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateExperience;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobApplication;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateAvailabilityDateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateExperienceRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobApplicationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobScheduleDateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateAIEvaluationService {

    private final CandidateRepository candidateRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateAvailabilityDateRepository availabilityDateRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobScheduleDateRepository jobScheduleDateRepository; // Add this field


    /**
     * Collects all necessary data for AI evaluation of a candidate for job applications in a group
     *
     * @param applicationGroupId The application group ID
     * @return Response containing collected data for AI evaluation
     */
    public Response<CandidateAIEvaluationDataDTO> collectCandidateEvaluationDataByGroupId(String applicationGroupId) {
        try {
            // Get all job applications in this group
            List<JobApplication> applications = jobApplicationRepository.findByApplicationGroupId(applicationGroupId);

            if (applications == null || applications.isEmpty()) {
                return new Response<>(404, "No applications found for this group ID", null);
            }

            // Use the first application to get candidate and job details
            JobApplication firstApplication = applications.get(0);

            // Get candidate
            Candidates candidate = firstApplication.getCandidate();
            if (candidate == null) {
                return new Response<>(404, "Candidate not found for this application group", null);
            }

            // Get job from the first application
            JobLocation jobLocation = firstApplication.getJobLocation();
            if (jobLocation == null) {
                return new Response<>(404, "Job location information missing", null);
            }

            Jobs job = jobLocation.getJob();
            if (job == null) {
                return new Response<>(404, "Job information missing", null);
            }

            // Get the total job working days - count all dates for this job schedule
            Long jobScheduleId = jobLocation.getJobScheduleDate().getJobSchedule().getId();
            Integer totalJobWorkingDays = jobScheduleDateRepository.countByJobScheduleId(jobScheduleId);

            // Get candidate experiences
            List<CandidateExperience> experiences = experienceRepository.findByCandidateId(candidate.getId());

            // Get candidate availability dates
            List<CandidateAvailabilityDate> availabilityDates =
                    availabilityDateRepository.findByCandidateId(candidate.getId());

            // Format dates for better readability
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Get all work dates from all applications in the group
            List<LocalDate> appliedWorkDates = applications.stream()
                    .map(app -> app.getJobLocation().getJobScheduleDate().getWorkDate())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            // Calculate average distance to candidate across all applications
            Double avgDistance = applications.stream()
                    .map(JobApplication::getDistanceToCandidate)
                    .filter(distance -> distance != null)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            if (avgDistance == 0.0) {
                avgDistance = null; // Set to null if no distance data available
            }

            // Build response DTO
            CandidateAIEvaluationDataDTO evaluationData = CandidateAIEvaluationDataDTO.builder()
                    .applicationGroupId(applicationGroupId)
                    .jobApplicationIds(applications.stream()
                            .map(JobApplication::getId)
                            .collect(Collectors.toList()))

                    // Candidate personal info
                    .candidateId(candidate.getId())
                    .candidateName(candidate.getName())
                    .dateOfBirth(candidate.getDateOfBirth())
                    .ethnicity(candidate.getRace() != null ? candidate.getRace().name() : null)
                    .gender(candidate.getGender() != null ? candidate.getGender().name() : null)
                    .bio(candidate.getBio())
                    .employmentStatus(candidate.getEmploymentStatus() != null ?
                            candidate.getEmploymentStatus().name() : null)
                    .languages(candidate.getLanguages() != null ?
                            candidate.getLanguages().stream()
                                    .map(lang -> lang.name())
                                    .collect(Collectors.toList()) :
                            null)

                    // Resume
                    .resumeUrl(candidate.getResumeUrl())

                    // Candidate availability
                    .availabilityType(candidate.getAvailability() != null ?
                            candidate.getAvailability().name() : null)
                    .availableDates(availabilityDates.stream()
                            .map(CandidateAvailabilityDate::getAvailableDate)
                            .map(date -> date.format(formatter))
                            .collect(Collectors.toList()))

                    // Work experience
                    .experiences(experiences.stream()
                            .map(exp -> new CandidateAIEvaluationDataDTO.ExperienceData(
                                    exp.getJobType() != null ? exp.getJobType().name() : null,
                                    exp.getExperienceText()))
                            .collect(Collectors.toList()))

                    // Job details
                    .jobId(job.getId())
                    .jobTitle(job.getTitle())
                    .jobTitleType(job.getJobTitleType() != null ? job.getJobTitleType().name() : null)
                    .jobScope(job.getJobScope())
                    .jobRequirements(job.getRequirements())
                    .salaryType(job.getSalaryType() != null ? job.getSalaryType().name() : null)

                    // Location info - collect all locations
                    .locationNames(applications.stream()
                            .map(app -> app.getJobLocation().getLocation().getName())
                            .distinct()
                            .collect(Collectors.toList()))
                    .applicationDate(firstApplication.getApplicationDate())
                    .distanceToCandidate(avgDistance)

                    // Work dates info
                    .appliedWorkDates(appliedWorkDates.stream()
                            .map(date -> date.format(formatter))
                            .collect(Collectors.toList()))
                    .totalWorkDays(appliedWorkDates.size())
                    .totalJobWorkingDays(totalJobWorkingDays)

                    .build();

            return new Response<>(200, "Candidate evaluation data collected successfully", evaluationData);

        } catch (Exception e) {
            log.error("Error collecting candidate evaluation data: {}", e.getMessage(), e);
            return new Response<>(500, "Error collecting candidate evaluation data: " + e.getMessage(), null);
        }
    }

    /**
     * Collect candidate evaluation data by a single job application ID.
     * If the application is part of a group, it collects data for all applications in the group.
     *
     * @param jobApplicationId The job application ID
     * @return Response with evaluation data
     */
    public Response<CandidateAIEvaluationDataDTO> collectCandidateEvaluationData(Long jobApplicationId) {
        try {
            // Get job application
            Optional<JobApplication> applicationOpt = jobApplicationRepository.findById(jobApplicationId);

            if (applicationOpt.isEmpty()) {
                return new Response<>(404, "Job application not found", null);
            }

            JobApplication application = applicationOpt.get();

            // Check if application is part of a group
            if (application.getApplicationGroupId() != null) {
                // If it's part of a group, use the group ID method
                return collectCandidateEvaluationDataByGroupId(application.getApplicationGroupId());
            }

            // Get candidate
            Candidates candidate = application.getCandidate();
            if (candidate == null) {
                return new Response<>(404, "Candidate not found for this application", null);
            }

            // Get job
            JobLocation jobLocation = application.getJobLocation();
            if (jobLocation == null) {
                return new Response<>(404, "Job location information missing", null);
            }

            Jobs job = jobLocation.getJob();
            if (job == null) {
                return new Response<>(404, "Job information missing", null);
            }

            // Get the total job working days - count all dates for this job schedule
            Long jobScheduleId = jobLocation.getJobScheduleDate().getJobSchedule().getId();
            Integer totalJobWorkingDays = jobScheduleDateRepository.countByJobScheduleId(jobScheduleId);

            // Get candidate experiences
            List<CandidateExperience> experiences = experienceRepository.findByCandidateId(candidate.getId());

            // Get candidate availability dates
            List<CandidateAvailabilityDate> availabilityDates =
                    availabilityDateRepository.findByCandidateId(candidate.getId());

            // Format dates for better readability
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Get work date
            LocalDate workDate = application.getJobLocation().getJobScheduleDate().getWorkDate();

            // Build response DTO
            CandidateAIEvaluationDataDTO evaluationData = CandidateAIEvaluationDataDTO.builder()
                    .applicationGroupId(null) // No group for single application
                    .jobApplicationIds(List.of(application.getId()))

                    // Candidate personal info
                    .candidateId(candidate.getId())
                    .candidateName(candidate.getName())
                    .dateOfBirth(candidate.getDateOfBirth())
                    .ethnicity(candidate.getRace() != null ? candidate.getRace().name() : null)
                    .gender(candidate.getGender() != null ? candidate.getGender().name() : null)
                    .bio(candidate.getBio())
                    .employmentStatus(candidate.getEmploymentStatus() != null ?
                            candidate.getEmploymentStatus().name() : null)
                    .languages(candidate.getLanguages() != null ?
                            candidate.getLanguages().stream()
                                    .map(lang -> lang.name())
                                    .collect(Collectors.toList()) :
                            null)

                    // Resume
                    .resumeUrl(candidate.getResumeUrl())

                    // Candidate availability
                    .availabilityType(candidate.getAvailability() != null ?
                            candidate.getAvailability().name() : null)
                    .availableDates(availabilityDates.stream()
                            .map(CandidateAvailabilityDate::getAvailableDate)
                            .map(date -> date.format(formatter))
                            .collect(Collectors.toList()))

                    // Work experience
                    .experiences(experiences.stream()
                            .map(exp -> new CandidateAIEvaluationDataDTO.ExperienceData(
                                    exp.getJobType() != null ? exp.getJobType().name() : null,
                                    exp.getExperienceText()))
                            .collect(Collectors.toList()))

                    // Job details
                    .jobId(job.getId())
                    .jobTitle(job.getTitle())
                    .jobTitleType(job.getJobTitleType() != null ? job.getJobTitleType().name() : null)
                    .jobScope(job.getJobScope())
                    .jobRequirements(job.getRequirements())
                    .salaryType(job.getSalaryType() != null ? job.getSalaryType().name() : null)

                    // Location info
                    .locationNames(List.of(application.getJobLocation().getLocation().getName()))
                    .applicationDate(application.getApplicationDate())
                    .distanceToCandidate(application.getDistanceToCandidate())

                    // Work dates info
                    .appliedWorkDates(List.of(workDate.format(formatter)))
                    .totalWorkDays(1)
                    .totalJobWorkingDays(totalJobWorkingDays)

                    .build();

            return new Response<>(200, "Candidate evaluation data collected successfully", evaluationData);

        } catch (Exception e) {
            log.error("Error collecting candidate evaluation data: {}", e.getMessage(), e);
            return new Response<>(500, "Error collecting candidate evaluation data: " + e.getMessage(), null);
        }
    }
}
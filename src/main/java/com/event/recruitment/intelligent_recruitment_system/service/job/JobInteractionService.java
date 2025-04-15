// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/job/JobInteractionService.java

package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobInteractionStatusResponse;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.ViewStatisticsResponse;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.SavedJob;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.ViewedJob;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.SavedJobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.ViewedJobRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobInteractionService {

    private final SavedJobRepository savedJobRepository;
    private final ViewedJobRepository viewedJobRepository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;
    private final SecurityUtil securityUtil;

    // ---------- Candidate Methods ----------

    /**
     * Save a job for the current candidate
     * @param jobId The job ID to save
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> saveJob(Long jobId) {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Check if job exists
            if (!jobRepository.existsById(jobId)) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Job not found", null);
            }

            // Check if already saved
            if (savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId)) {
                return new Response<>(HttpStatus.OK.value(), "Job already saved", null);
            }

            // Save the job
            SavedJob savedJob = new SavedJob();
            savedJob.setCandidateId(candidateId);
            savedJob.setJobId(jobId);
            savedJob.setSavedAt(LocalDateTime.now());

            savedJobRepository.save(savedJob);

            return new Response<>(HttpStatus.OK.value(), "Job saved successfully", null);
        } catch (Exception e) {
            log.error("Error saving job", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error saving job: " + e.getMessage(), null);
        }
    }

    /**
     * Remove a job from the current candidate's saved list
     * @param jobId The job ID to unsave
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> unsaveJob(Long jobId) {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Find the saved job
            Optional<SavedJob> savedJobOptional = savedJobRepository.findByCandidateIdAndJobId(candidateId, jobId);

            if (savedJobOptional.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Job not saved", null);
            }

            // Delete the saved job
            savedJobRepository.delete(savedJobOptional.get());

            return new Response<>(HttpStatus.OK.value(), "Job removed from saved list", null);
        } catch (Exception e) {
            log.error("Error removing saved job", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error removing saved job: " + e.getMessage(), null);
        }
    }

    /**
     * Record a job view for the current candidate
     * @param jobId The job ID that was viewed
     * @return Response indicating success or failure
     */
    @Transactional
    public Response<?> viewJob(Long jobId) {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Check if job exists
            if (!jobRepository.existsById(jobId)) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Job not found", null);
            }

            // Check if already viewed
            Optional<ViewedJob> viewedJobOptional = viewedJobRepository.findByCandidateIdAndJobId(candidateId, jobId);

            if (viewedJobOptional.isPresent()) {
                // Update existing viewed job record
                ViewedJob viewedJob = viewedJobOptional.get();
                viewedJob.setViewCount(viewedJob.getViewCount() + 1);
                viewedJob.setLastViewedAt(LocalDateTime.now());
                viewedJobRepository.save(viewedJob);
            } else {
                // Create new viewed job record
                ViewedJob viewedJob = new ViewedJob();
                viewedJob.setCandidateId(candidateId);
                viewedJob.setJobId(jobId);
                viewedJob.setViewedAt(LocalDateTime.now());
                viewedJob.setViewCount(1);
                viewedJob.setLastViewedAt(LocalDateTime.now());
                viewedJobRepository.save(viewedJob);
            }

            return new Response<>(HttpStatus.OK.value(), "Job view recorded", null);
        } catch (Exception e) {
            log.error("Error recording job view", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error recording job view: " + e.getMessage(), null);
        }
    }

    /**
     * Get the current candidate's interaction status for a specific job
     * @param jobId The job ID to check
     * @return Response with saved and viewed status
     */
    public Response<?> getInteractionStatus(Long jobId) {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            boolean saved = savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId);
            boolean viewed = viewedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId);

            JobInteractionStatusResponse response = new JobInteractionStatusResponse(saved, viewed);

            return new Response<>(HttpStatus.OK.value(), "Interaction status retrieved", response);
        } catch (Exception e) {
            log.error("Error getting interaction status", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error getting interaction status: " + e.getMessage(), null);
        }
    }

    /**
     * Get all jobs saved by the current candidate
     * @return Response with list of saved jobs
     */
    public Response<?> getSavedJobs() {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Get all saved job IDs
            List<Long> savedJobIds = savedJobRepository.findByCandidateId(candidateId)
                    .stream()
                    .map(SavedJob::getJobId)
                    .collect(Collectors.toList());

            // Get job details for each saved job
            List<JobSummaryResponseDTO> savedJobDetails = new ArrayList<>();
            if (!savedJobIds.isEmpty()) {
                // Get job details by ID list
                // This is a placeholder - you'll need to implement this method in your JobService
                // savedJobDetails = jobService.getJobDetailsByIds(savedJobIds);

                // For each job, set the saved and viewed flags
                for (JobSummaryResponseDTO job : savedJobDetails) {
                    job.setSaved(true);
                    job.setViewed(viewedJobRepository.existsByCandidateIdAndJobId(candidateId, job.getId()));
                }
            }

            return new Response<>(HttpStatus.OK.value(), "Saved jobs retrieved", savedJobDetails);
        } catch (Exception e) {
            log.error("Error retrieving saved jobs", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error retrieving saved jobs: " + e.getMessage(), null);
        }
    }

    /**
     * Get all jobs viewed by the current candidate
     * @return Response with list of viewed jobs
     */
    public Response<?> getViewedJobs() {
        try {
            // Get current logged-in candidate ID
            Long candidateId = securityUtil.getCurrentCandidateId();
            if (candidateId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Get all viewed job IDs, ordered by last viewed time
            List<Long> viewedJobIds = viewedJobRepository.findByCandidateIdOrderByLastViewedAtDesc(candidateId)
                    .stream()
                    .map(ViewedJob::getJobId)
                    .collect(Collectors.toList());

            // Get job details for each viewed job
            List<JobSummaryResponseDTO> viewedJobDetails = new ArrayList<>();
            if (!viewedJobIds.isEmpty()) {
                // Get job details by ID list
                // This is a placeholder - you'll need to implement this method in your JobService
                // viewedJobDetails = jobService.getJobDetailsByIds(viewedJobIds);

                // For each job, set the saved and viewed flags
                for (JobSummaryResponseDTO job : viewedJobDetails) {
                    job.setViewed(true);
                    job.setSaved(savedJobRepository.existsByCandidateIdAndJobId(candidateId, job.getId()));
                }
            }

            return new Response<>(HttpStatus.OK.value(), "Viewed jobs retrieved", viewedJobDetails);
        } catch (Exception e) {
            log.error("Error retrieving viewed jobs", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error retrieving viewed jobs: " + e.getMessage(), null);
        }
    }

    // ---------- Recruiter Methods ----------

    /**
     * Get view statistics for a specific job
     * @param jobId The job ID to get statistics for
     * @return Response with view statistics
     */
    public Response<?> getJobViewStatistics(Long jobId) {
        try {
            // Validate that the current recruiter owns this job
            Long recruiterId = securityUtil.getCurrentRecruiterId();
            if (recruiterId == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(), "Authentication required", null);
            }

            // Check if job exists and belongs to this recruiter
            // This validation depends on your specific implementation
            boolean jobBelongsToRecruiter = jobRepository.findById(jobId)
                    .map(job -> job.getProject().getRecruiter().getId().equals(recruiterId))
                    .orElse(false);

            if (!jobBelongsToRecruiter) {
                return new Response<>(HttpStatus.FORBIDDEN.value(), "You don't have permission to access this job", null);
            }

            // Get total unique viewers
            Long totalViewers = viewedJobRepository.countUniqueViewersByJobId(jobId);

            // Get recent viewers with details
            List<ViewedJob> recentViews = viewedJobRepository.findAllViewsForJob(jobId);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            List<ViewStatisticsResponse.ViewerDetail> viewerDetails = recentViews.stream()
                    .map(view -> {
                        // Find candidate info
                        Optional<Candidates> candidateOpt = candidateRepository.findById(view.getCandidateId());
                        String candidateName = candidateOpt.map(Candidates::getName).orElse("Unknown");

                        return new ViewStatisticsResponse.ViewerDetail(
                                view.getCandidateId(),
                                candidateName,
                                view.getLastViewedAt().format(formatter),
                                view.getViewCount()
                        );
                    })
                    .collect(Collectors.toList());

            ViewStatisticsResponse response = new ViewStatisticsResponse(
                    jobId,
                    totalViewers,
                    viewerDetails
            );

            return new Response<>(HttpStatus.OK.value(), "Job view statistics retrieved", response);
        } catch (Exception e) {
            log.error("Error retrieving job view statistics", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error retrieving job view statistics: " + e.getMessage(), null);
        }
    }

    // ---------- Helper Methods ----------

    /**
     * Enhances job listings with interaction status for the current candidate
     * @param jobs List of job summary DTOs to enhance
     */
    public void enhanceJobListingsWithInteractionStatus(List<JobSummaryResponseDTO> jobs) {
        try {
            // Try to get current candidate ID
            Long candidateId = null;
            try {
                candidateId = securityUtil.getCurrentCandidateId();
            } catch (Exception e) {
                // Not a candidate or not authenticated, return without enhancement
                return;
            }

            if (candidateId == null) {
                return; // Not a candidate, no enhancement needed
            }

            // Get all saved and viewed job IDs for the current candidate
            List<Long> savedJobIds = savedJobRepository.findByCandidateId(candidateId)
                    .stream()
                    .map(SavedJob::getJobId)
                    .collect(Collectors.toList());

            List<Long> viewedJobIds = viewedJobRepository.findByCandidateIdOrderByLastViewedAtDesc(candidateId)
                    .stream()
                    .map(ViewedJob::getJobId)
                    .collect(Collectors.toList());

            // Enhance each job with interaction status
            for (JobSummaryResponseDTO job : jobs) {
                job.setSaved(savedJobIds.contains(job.getId()));
                job.setViewed(viewedJobIds.contains(job.getId()));
            }
        } catch (Exception e) {
            // Log error but continue without enhancement
            log.error("Error enhancing job listings: " + e.getMessage(), e);
        }
    }
}
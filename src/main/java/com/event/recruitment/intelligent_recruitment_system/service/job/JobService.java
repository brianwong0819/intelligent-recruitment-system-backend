// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/job/JobService.java

package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.ChangeJobStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.CreateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.UpdateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.ProjectRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public Response<JobResponseDTO> createJob(CreateJobRequest request) {
        try {
            // Verify that project exists and belongs to the current recruiter
            Projects project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check if the project belongs to the current recruiter
            if (!securityUtil.getCurrentRecruiterId().equals(project.getRecruiter().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(), "You don't have permission to create jobs for this project", null);
            }

            // Create job entity
            Jobs job = Jobs.builder()
                    .project(project)
                    .title(request.getTitle())
                    .jobTitleType(request.getJobTitleType())
                    .jobScope(request.getJobScope())
                    .requirements(request.getRequirements())
                    .salary(request.getSalary())
                    .paymentTerms(request.getPaymentTerms()) // Add payment terms
                    .salaryType(request.getSalaryType())
                    .benefits(request.getBenefits())
                    .status(JobStatusType.DRAFT)
                    .build();

            // Save job
            Jobs savedJob = jobRepository.save(job);

            // Map to response
            JobResponseDTO responseDTO = mapToJobResponseDTO(savedJob);

            return new Response<>(HttpStatus.CREATED.value(), "Job created successfully", responseDTO);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create job: " + e.getMessage(), null);
        }
    }

    public Response<JobResponseDTO> getJobById(Long jobId) {
        try {
            Jobs job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Check if the job belongs to the current recruiter
            if (!securityUtil.getCurrentRecruiterId().equals(job.getProject().getRecruiter().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(), "You don't have permission to view this job", null);
            }

            JobResponseDTO responseDTO = mapToJobResponseDTO(job);
            return new Response<>(HttpStatus.OK.value(), "Job retrieved successfully", responseDTO);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve job: " + e.getMessage(), null);
        }
    }

    public Response<List<JobResponseDTO>> getJobsByProjectId(Long projectId) {
        try {
            // Verify that project exists and belongs to the current recruiter
            Projects project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Check if the project belongs to the current recruiter
            if (!securityUtil.getCurrentRecruiterId().equals(project.getRecruiter().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(), "You don't have permission to view jobs for this project", null);
            }

            List<Jobs> jobs = jobRepository.findByProjectId(projectId);
            List<JobResponseDTO> responseDTOs = jobs.stream()
                    .map(this::mapToJobResponseDTO)
                    .collect(Collectors.toList());

            return new Response<>(HttpStatus.OK.value(), "Jobs retrieved successfully", responseDTOs);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve jobs: " + e.getMessage(), null);
        }
    }

    public Response<List<JobResponseDTO>> getAllJobs() {
        try {
            Long recruiterId = securityUtil.getCurrentRecruiterId();

            // Get all projects for the current recruiter
            List<Projects> recruiterProjects = projectRepository.findByRecruiterId(recruiterId);

            // Extract all jobs from these projects
            List<Jobs> allJobs = recruiterProjects.stream()
                    .flatMap(project -> jobRepository.findByProject(project).stream())
                    .collect(Collectors.toList());

            List<JobResponseDTO> responseDTOs = allJobs.stream()
                    .map(this::mapToJobResponseDTO)
                    .collect(Collectors.toList());

            return new Response<>(HttpStatus.OK.value(), "All jobs retrieved successfully", responseDTOs);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve jobs: " + e.getMessage(), null);
        }
    }

    /**
     * Update job details
     *
     * @param jobId  The ID of the job to update
     * @param request Contains updated job information
     * @return Response with updated job details
     */
    @Transactional
    public Response<JobResponseDTO> updateJob(Long jobId, UpdateJobRequest request) {
        try {
            // Find the job
            Jobs job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Check if the job belongs to the current recruiter
            if (!securityUtil.getCurrentRecruiterId().equals(job.getProject().getRecruiter().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You don't have permission to update this job",
                        null);
            }

            // Check if job status allows updates
            if (List.of(JobStatusType.FILLED, JobStatusType.CANCELLED, JobStatusType.ARCHIVED)
                    .contains(job.getStatus())) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Cannot update job in " + job.getStatus() + " status",
                        null);
            }

            // Update job entity
            job.setTitle(request.getTitle());
            job.setJobTitleType(request.getJobTitleType());
            job.setJobScope(request.getJobScope());
            job.setRequirements(request.getRequirements());
            job.setSalary(request.getSalary());
            job.setPaymentTerms(request.getPaymentTerms());
            job.setSalaryType(request.getSalaryType());
            job.setBenefits(request.getBenefits());

            // Save updated job
            Jobs updatedJob = jobRepository.save(job);

            // Map to response
            JobResponseDTO responseDTO = mapToJobResponseDTO(updatedJob);

            return new Response<>(HttpStatus.OK.value(), "Job updated successfully", responseDTO);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to update job: " + e.getMessage(),
                    null);
        }
    }

    private JobResponseDTO mapToJobResponseDTO(Jobs job) {
        return JobResponseDTO.builder()
                .id(job.getId())
                .projectId(job.getProject().getId())
                .projectName(job.getProject().getName())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .jobScope(job.getJobScope())
                .requirements(job.getRequirements())
                .salary(job.getSalary())
                .paymentTerms(job.getPaymentTerms()) // Add payment terms
                .salaryType(job.getSalaryType())
                .benefits(job.getBenefits())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .build();
    }

    /**
     * Change the status of a job
     *
     * @param request Contains job ID and new status
     * @return Response with updated job details
     */
    @Transactional
    public Response<JobResponseDTO> changeJobStatus(ChangeJobStatusRequest request) {
        try {
            // Find the job
            Jobs job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found"));

            // Check if the job belongs to the current recruiter
            if (!securityUtil.getCurrentRecruiterId().equals(job.getProject().getRecruiter().getId())) {
                return new Response<>(HttpStatus.FORBIDDEN.value(),
                        "You don't have permission to change the status of this job",
                        null);
            }

            // Validate status change
            validateStatusChange(job.getStatus(), request.getNewStatus());

            // Update job status
            job.setStatus(request.getNewStatus());

            // Save updated job
            Jobs updatedJob = jobRepository.save(job);

            // Map to response DTO
            JobResponseDTO responseDTO = mapToJobResponseDTO(updatedJob);

            return new Response<>(HttpStatus.OK.value(),
                    "Job status changed successfully",
                    responseDTO);
        } catch (IllegalArgumentException e) {
            return new Response<>(HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    null);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to change job status: " + e.getMessage(),
                    null);
        }
    }

    /**
     * Validate status changes based on business rules
     *
     * @param currentStatus Current job status
     * @param newStatus     Proposed new status
     * @throws IllegalArgumentException if status change is not allowed
     */
    private void validateStatusChange(JobStatusType currentStatus, JobStatusType newStatus) {
        // Define allowed status transitions
        switch (currentStatus) {
            case DRAFT:
                // From DRAFT, can go to OPEN, CANCELLED, or PENDING_REVIEW
                if (!List.of(JobStatusType.OPEN, JobStatusType.CANCELLED, JobStatusType.PENDING_REVIEW)
                        .contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from DRAFT");
                }
                break;
            case OPEN:
                // From OPEN, can go to CLOSED, FILLED, CANCELLED, or ARCHIVED
                if (!List.of(JobStatusType.CLOSED, JobStatusType.FILLED,
                                JobStatusType.CANCELLED, JobStatusType.ARCHIVED)
                        .contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from OPEN");
                }
                break;
            case PENDING_REVIEW:
                // From PENDING_REVIEW, can go to OPEN, CANCELLED
                if (!List.of(JobStatusType.OPEN, JobStatusType.CANCELLED)
                        .contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from PENDING_REVIEW");
                }
                break;
            case CLOSED:
                // From CLOSED, can go to ARCHIVED or OPEN
                if (!List.of(JobStatusType.ARCHIVED, JobStatusType.OPEN)
                        .contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from CLOSED");
                }
                break;
            case FILLED:
                // From FILLED, can go to CLOSED or ARCHIVED
                if (!List.of(JobStatusType.CLOSED, JobStatusType.ARCHIVED)
                        .contains(newStatus)) {
                    throw new IllegalArgumentException("Invalid status transition from FILLED");
                }
                break;
            case CANCELLED:
                // CANCELLED is typically a final state
                throw new IllegalArgumentException("Cannot change status from CANCELLED");
            case ARCHIVED:
                // ARCHIVED is typically a final state
                throw new IllegalArgumentException("Cannot change status from ARCHIVED");
            default:
                throw new IllegalArgumentException("Unknown current status");
        }
    }
}
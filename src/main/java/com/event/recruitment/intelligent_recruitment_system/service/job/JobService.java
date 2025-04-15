// src/main/java/com/event/recruitment/intelligent_recruitment_system/service/job/JobService.java

package com.event.recruitment.intelligent_recruitment_system.service.job;

import com.event.recruitment.intelligent_recruitment_system.dto.common.PagedResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.ChangeJobStatusRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.CreateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.JobListFilterRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.job.UpdateJobRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobScheduleResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.job.JobSummaryResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobSchedule;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobScheduleDate;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobStatusType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobSpecification;
import com.event.recruitment.intelligent_recruitment_system.repository.location.LocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.ProjectRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final SecurityUtil securityUtil;
    private final JobMapper jobMapper;
    private final CandidateRepository candidateRepository;
    private final LocationRepository locationRepository;
    private final JobInteractionService jobInteractionService;


    /**
     * Create a new job
     *
     * @param request Job creation request
     * @return Response with created job details
     */
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
                    .paymentTerms(request.getPaymentTerms())
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

    /**
     * Get job by ID (protected, requires authentication)
     *
     * @param jobId Job ID
     * @return Response with job details
     */
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

    /**
     * Get public job details (public, no authentication required)
     *
     * @param jobId Job ID
     * @return Response with job details
     */
    @Transactional
    public Response<JobResponseDTO> getPublicJobDetails(Long jobId) {
        try {
            Optional<Jobs> jobOptional = jobRepository.findByIdWithAllDetails(jobId);

            if (jobOptional.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Job not found", null);
            }

            Jobs job = jobOptional.get();

            // Verify that job is in OPEN status
            if (job.getStatus() != JobStatusType.OPEN) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Job is not available", null);
            }

            // Manually initialize the collections to avoid LazyInitializationException
            forceInitialization(job);

            JobResponseDTO responseDTO = mapToJobResponseDTO(job);
            return new Response<>(HttpStatus.OK.value(), "Job retrieved successfully", responseDTO);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve job: " + e.getMessage(), null);
        }
    }

    /**
     * Force initialization of lazy-loaded collections
     * @param job The job entity to initialize
     */
    private void forceInitialization(Jobs job) {
        if (job.getJobSchedules() != null) {
            Hibernate.initialize(job.getJobSchedules());

            for (JobSchedule schedule : job.getJobSchedules()) {
                if (schedule.getScheduleDates() != null) {
                    Hibernate.initialize(schedule.getScheduleDates());

                    for (JobScheduleDate date : schedule.getScheduleDates()) {
                        if (date.getJobLocations() != null) {
                            Hibernate.initialize(date.getJobLocations());

                            // Initialize location
                            for (JobLocation location : date.getJobLocations()) {
                                if (location.getLocation() != null) {
                                    Hibernate.initialize(location.getLocation());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get jobs by project ID
     *
     * @param projectId Project ID
     * @return Response with list of jobs
     */
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

    /**
     * Get all jobs for the current recruiter
     *
     * @return Response with list of jobs
     */
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
     * Get jobs with filters (public endpoint)
     *
     * @param filterRequest Filter criteria
     * @return Response with paged job summary results
     */
    public Response<PagedResponseDTO<JobSummaryResponseDTO>> getJobsWithFilters(JobListFilterRequest filterRequest) {
        try {
            // Set default pagination values if not provided
            int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
            int size = filterRequest.getSize() != null ? filterRequest.getSize() : 10;

            // Set default sorting if not provided
            String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "createdAt";
            String sortDirection = filterRequest.getSortDirection() != null ? filterRequest.getSortDirection() : "desc";

            // Create pageable object
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Create specification
            Specification<Jobs> spec = JobSpecification.getJobsWithFilters(filterRequest);

            // Execute query
            Page<Jobs> jobsPage = jobRepository.findAll(spec, pageable);

            // Map to DTOs
            List<JobSummaryResponseDTO> jobSummaries = jobsPage.getContent().stream()
                    .map(job -> {
                        JobSummaryResponseDTO dto = jobMapper.toSummaryDTO(job);

                        // Calculate distances if latitude/longitude provided
                        if (filterRequest.getLatitude() != null &&
                                filterRequest.getLongitude() != null &&
                                filterRequest.getDistance() != null) {

                            Double distance = jobMapper.calculateDistanceToClosestLocation(
                                    job,
                                    filterRequest.getLatitude(),
                                    filterRequest.getLongitude()
                            );

                            if (distance != null) {
                                dto.setDistance(distance);
                            }
                        }

                        return dto;
                    })
                    .collect(Collectors.toList());

            jobInteractionService.enhanceJobListingsWithInteractionStatus(jobSummaries);

            // Create paged response
            PagedResponseDTO<JobSummaryResponseDTO> pagedResponse = PagedResponseDTO.<JobSummaryResponseDTO>builder()
                    .content(jobSummaries)
                    .page(jobsPage.getNumber())
                    .size(jobsPage.getSize())
                    .totalElements(jobsPage.getTotalElements())
                    .totalPages(jobsPage.getTotalPages())
                    .last(jobsPage.isLast())
                    .build();

            return new Response<>(HttpStatus.OK.value(), "Jobs retrieved successfully", pagedResponse);
        } catch (Exception e) {
            log.error("Error retrieving filtered jobs", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve jobs: " + e.getMessage(), null);
        }
    }

    /**
     * Get jobs near the candidate's preferred location
     *
     * @param distance Distance in kilometers to search around the candidate's preferred location
     * @param page Page number for pagination
     * @param size Page size for pagination
     * @param sortBy Field to sort by
     * @param sortDirection Direction to sort (asc/desc)
     * @return Response with paged job summary results
     */
    public Response<PagedResponseDTO<JobSummaryResponseDTO>> getJobsNearCandidateLocation(
            Double distance, Integer page, Integer size, String sortBy, String sortDirection) {
        try {
            // Get the current candidate from security context
            String username = securityUtil.getCurrentUsername();
            if (username == null) {
                return new Response<>(HttpStatus.UNAUTHORIZED.value(),
                        "Authentication required", null);
            }

            // Fetch the candidate and their preferred location
            Candidates candidate = candidateRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));

            // Check if candidate has a preferred location
            Location preferredLocation = candidate.getPreferredLocation();
            if (preferredLocation == null) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "No preferred location set. Please update your profile with a preferred location.", null);
            }

            if (preferredLocation.getLatitude() == null || preferredLocation.getLongitude() == null) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(),
                        "Invalid location coordinates. Please update your preferred location.", null);
            }

            // Create filter request with candidate's location parameters
            JobListFilterRequest filterRequest = JobListFilterRequest.builder()
                    .page(page)
                    .size(size)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .latitude(preferredLocation.getLatitude().doubleValue()) // Convert BigDecimal to double
                    .longitude(preferredLocation.getLongitude().doubleValue()) // Convert BigDecimal to double
                    .distance(distance)
                    .build();

            // Use the existing filter method to get jobs near the candidate's location
            return getJobsWithFilters(filterRequest);
        } catch (Exception e) {
            log.error("Error getting jobs near candidate location", e);
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to retrieve jobs near your location: " + e.getMessage(), null);
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
     * Maps a job entity to a detailed response DTO including schedules, dates, and locations.
     */
    private JobResponseDTO mapToJobResponseDTO(Jobs job) {
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

        // Map job schedules
        List<JobScheduleResponseDTO> scheduleResponseDTOs = new ArrayList<>();
        if (job.getJobSchedules() != null && !job.getJobSchedules().isEmpty()) {
            scheduleResponseDTOs = job.getJobSchedules().stream()
                    .map(jobSchedule -> {
                        // Map schedule dates
                        List<JobScheduleResponseDTO.JobScheduleDateResponseDTO> scheduleDateDTOs =
                                jobSchedule.getScheduleDates().stream()
                                        .map(scheduleDate -> {
                                            // Map job locations
                                            List<JobScheduleResponseDTO.JobLocationResponseDTO> locationDTOs =
                                                    scheduleDate.getJobLocations().stream()
                                                            .map(jobLocation -> JobScheduleResponseDTO.JobLocationResponseDTO.builder()
                                                                    .id(jobLocation.getId())
                                                                    .locationId(jobLocation.getLocation().getId())
                                                                    .locationName(jobLocation.getLocation().getName())
                                                                    .positionsNeeded(jobLocation.getPositionsNeeded())
                                                                    .positionsFilled(jobLocation.getPositionsFilled())
                                                                    .status(jobLocation.getStatus().name())
                                                                    .notes(jobLocation.getNotes())
                                                                    .build())
                                                            .collect(Collectors.toList());

                                            return JobScheduleResponseDTO.JobScheduleDateResponseDTO.builder()
                                                    .id(scheduleDate.getId())
                                                    .workDate(scheduleDate.getWorkDate())
                                                    .jobLocations(locationDTOs)
                                                    .build();
                                        })
                                        .collect(Collectors.toList());

                        return JobScheduleResponseDTO.builder()
                                .id(jobSchedule.getId())
                                .jobId(jobSchedule.getJob().getId())
                                .startDate(jobSchedule.getStartDate())
                                .endDate(jobSchedule.getEndDate())
                                .startTime(jobSchedule.getStartTime())
                                .endTime(jobSchedule.getEndTime())
                                .hoursOfRestTime(jobSchedule.getHoursOfRestTime())
                                .numPositions(jobSchedule.getNumPositions())
                                .scheduleDates(scheduleDateDTOs)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return JobResponseDTO.builder()
                .id(job.getId())
                .projectId(job.getProject().getId())
                .projectName(job.getProject().getName())
                .title(job.getTitle())
                .jobTitleType(job.getJobTitleType())
                .jobScope(job.getJobScope())
                .requirements(job.getRequirements())
                .salary(job.getSalary())
                .paymentTerms(job.getPaymentTerms())
                .salaryType(job.getSalaryType())
                .benefits(job.getBenefits())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                // Add company/recruiter information
                .recruiterId(job.getProject().getRecruiter().getId())
                .companyName(companyName)
                .companyLogoUrl(job.getProject().getRecruiter().getCompanyLogoUrl())
                .recruiterType(recruiterType)
                // Add job schedules
                .jobSchedules(scheduleResponseDTOs)
                .build();
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
package com.event.recruitment.intelligent_recruitment_system.service.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CreateProjectRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.UpdateProjectRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.ProjectResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.ProjectStatsDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.JobLocation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.job.Jobs;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Projects;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.model.enums.JobLocationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobLocationRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.job.JobRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.ProjectRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobRepository jobRepository;
    private final JobLocationRepository jobLocationRepository;
    private final SecurityUtil securityUtil;

    /**
     * Create a new project for the current authenticated recruiter
     *
     * @param request Project creation request with name and description
     * @return Response with the created project
     */
    @Transactional
    public Response<ProjectResponseDTO> createProject(CreateProjectRequest request) {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Create and save the new project
            Projects project = Projects.builder()
                    .recruiter(recruiter)
                    .name(request.getName())
                    .description(request.getDescription())
                    .build();

            Projects savedProject = projectRepository.save(project);

            // Convert to DTO and return
            ProjectResponseDTO responseDTO = mapToDTO(savedProject);
            return new Response<>(HttpStatus.CREATED.value(), "Project created successfully", responseDTO);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to create project: " + e.getMessage(), null);
        }
    }

    /**
     * Get all projects for the current authenticated recruiter
     *
     * @return Response with list of projects
     */
    public Response<List<ProjectResponseDTO>> getRecruiterProjects() {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Get all projects for the recruiter
            List<Projects> projects = projectRepository.findByRecruiter(recruiter);

            // Convert to DTOs
            List<ProjectResponseDTO> projectDTOs = projects.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return new Response<>(HttpStatus.OK.value(), "Projects retrieved successfully", projectDTOs);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve projects: " + e.getMessage(), null);
        }
    }

    /**
     * Get a specific project by ID for the current authenticated recruiter
     *
     * @param projectId Project ID to retrieve
     * @return Response with the project details
     */
    public Response<ProjectResponseDTO> getProjectById(Long projectId) {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Get the project if it belongs to the recruiter
            Projects project = projectRepository.findByIdAndRecruiterId(projectId, recruiter.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found or does not belong to this recruiter"));

            // Convert to DTO and return
            ProjectResponseDTO responseDTO = mapToDTO(project);
            return new Response<>(HttpStatus.OK.value(), "Project retrieved successfully", responseDTO);
        } catch (IllegalStateException e) {
            return new Response<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve project: " + e.getMessage(), null);
        }
    }

    /**
     * Get statistics for a specific project
     *
     * @param projectId Project ID to get statistics for
     * @return Response with project statistics
     */
    @Transactional(readOnly = true)
    public Response<ProjectStatsDTO> getProjectStatistics(Long projectId) {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Get the project if it belongs to the recruiter
            Projects project = projectRepository.findByIdAndRecruiterId(projectId, recruiter.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found or does not belong to this recruiter"));

            // Get all jobs for this project
            List<Jobs> jobs = jobRepository.findByProjectId(projectId);

            // Initialize statistics
            int totalJobs = jobs.size();
            int totalPositionsNeeded = 0;
            int totalPositionsFilled = 0;
            int totalWorkingDays = 0;
            int openJobs = 0;
            int filledJobs = 0;
            int partiallyFilledJobs = 0;
            int cancelledJobs = 0;

            // For tracking unique locations
            Set<Long> uniqueLocationIds = new HashSet<>();
            int totalLocations = 0;

            // Calculate statistics from job locations
            for (Jobs job : jobs) {
                List<JobLocation> jobLocations = jobLocationRepository.findByJobId(job.getId());

                totalLocations += jobLocations.size();

                // Track unique location IDs
                for (JobLocation jobLocation : jobLocations) {
                    uniqueLocationIds.add(jobLocation.getLocation().getId());

                    // Add positions needed/filled
                    Integer positionsNeeded = jobLocation.getPositionsNeeded();
                    if (positionsNeeded == null && jobLocation.getJobScheduleDate() != null &&
                            jobLocation.getJobScheduleDate().getJobSchedule() != null) {
                        // Fall back to the job schedule's positions if job location doesn't specify
                        positionsNeeded = jobLocation.getJobScheduleDate().getJobSchedule().getNumPositions();
                    }

                    if (positionsNeeded != null) {
                        totalPositionsNeeded += positionsNeeded;
                    }

                    if (jobLocation.getPositionsFilled() != null) {
                        totalPositionsFilled += jobLocation.getPositionsFilled();
                    }

                    // Count job statuses
                    if (jobLocation.getStatus() != null) {
                        switch (jobLocation.getStatus()) {
                            case OPEN:
                                openJobs++;
                                break;
                            case FILLED:
                                filledJobs++;
                                break;
                            case PARTIAL_FILLED:
                                partiallyFilledJobs++;
                                break;
                            case CANCELLED:
                                cancelledJobs++;
                                break;
                        }
                    }

                    // Count working days
                    if (jobLocation.getJobScheduleDate() != null) {
                        totalWorkingDays++;
                    }
                }
            }

            // Build the statistics DTO
            ProjectStatsDTO statsDTO = ProjectStatsDTO.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .totalJobs(totalJobs)
                    .totalLocations(totalLocations)
                    .totalUniqueLocations(uniqueLocationIds.size())
                    .totalPositionsNeeded(totalPositionsNeeded)
                    .totalPositionsFilled(totalPositionsFilled)
                    .totalWorkingDays(totalWorkingDays)
                    .openJobs(openJobs)
                    .filledJobs(filledJobs)
                    .partiallyFilledJobs(partiallyFilledJobs)
                    .cancelledJobs(cancelledJobs)
                    .build();

            return new Response<>(HttpStatus.OK.value(), "Project statistics retrieved successfully", statsDTO);
        } catch (IllegalStateException e) {
            return new Response<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve project statistics: " + e.getMessage(), null);
        }
    }

    /**
     * Delete a project (soft delete)
     *
     * @param projectId Project ID to delete
     * @return Response indicating the result of deletion
     */
    @Transactional
    public Response<Void> deleteProject(Long projectId) {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Find the project and verify it belongs to the recruiter
            Projects project = projectRepository.findByIdAndRecruiterId(projectId, recruiter.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found or does not belong to this recruiter"));

            // Check if all jobs in the project are cancelled
            boolean allJobsCancelled = projectRepository.areAllJobsCancelled(projectId);

            if (!allJobsCancelled) {
                return new Response<>(
                        HttpStatus.BAD_REQUEST.value(),
                        "Cannot delete project. Please cancel all associated jobs first.",
                        null
                );
            }

            // Soft delete the project
            project.setIsDeleted(true);
            projectRepository.save(project);

            return new Response<>(
                    HttpStatus.OK.value(),
                    "Project soft deleted successfully",
                    null
            );
        } catch (IllegalStateException e) {
            return new Response<>(
                    HttpStatus.NOT_FOUND.value(),
                    e.getMessage(),
                    null
            );
        } catch (Exception e) {
            return new Response<>(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to delete project: " + e.getMessage(),
                    null
            );
        }
    }

    // Add this method to ProjectService.java
    /**
     * Update an existing project for the current authenticated recruiter
     *
     * @param request Project update request with id, name and description
     * @return Response with the updated project
     */
    @Transactional
    public Response<ProjectResponseDTO> updateProject(UpdateProjectRequest request) {
        try {
            // Get the current authenticated recruiter
            String username = securityUtil.getCurrentUsername();
            Recruiters recruiter = recruiterRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalStateException("Current authenticated user not found"));

            // Get the project if it belongs to the recruiter
            Projects project = projectRepository.findByIdAndRecruiterId(request.getId(), recruiter.getId())
                    .orElseThrow(() -> new IllegalStateException("Project not found or does not belong to this recruiter"));

            // Update the project fields
            project.setName(request.getName());
            project.setDescription(request.getDescription());

            // Save the updated project
            Projects updatedProject = projectRepository.save(project);

            // Convert to DTO and return
            ProjectResponseDTO responseDTO = mapToDTO(updatedProject);
            return new Response<>(HttpStatus.OK.value(), "Project updated successfully", responseDTO);
        } catch (IllegalStateException e) {
            return new Response<>(HttpStatus.NOT_FOUND.value(), e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to update project: " + e.getMessage(), null);
        }
    }

    /**
     * Helper method to map Project entity to ProjectResponseDTO
     */
    private ProjectResponseDTO mapToDTO(Projects project) {
        // Convert Long to int, handling potential overflow
        int jobCount = jobRepository.countByProjectId(project.getId()).intValue();

        return ProjectResponseDTO.builder()
                .id(project.getId())
                .recruiterId(project.getRecruiter().getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdAt(project.getCreatedAt())
                .isDeleted(project.getIsDeleted())
                .deletedAt(project.getDeletedAt())
                .jobCount(jobCount)
                .build();
    }
}
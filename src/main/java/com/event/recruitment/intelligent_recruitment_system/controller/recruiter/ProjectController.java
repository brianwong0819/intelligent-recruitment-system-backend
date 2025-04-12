package com.event.recruitment.intelligent_recruitment_system.controller.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CreateProjectRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.UpdateProjectRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.ProjectResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.ProjectStatsDTO;
import com.event.recruitment.intelligent_recruitment_system.service.recruiter.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<Response<ProjectResponseDTO>> createProject(@Valid @RequestBody CreateProjectRequest request) {
        try {
            Response<ProjectResponseDTO> response = projectService.createProject(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error creating project: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<Response<List<ProjectResponseDTO>>> getRecruiterProjects() {
        try {
            Response<List<ProjectResponseDTO>> response = projectService.getRecruiterProjects();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving projects: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Response<ProjectResponseDTO>> getProjectById(@PathVariable Long projectId) {
        try {
            Response<ProjectResponseDTO> response = projectService.getProjectById(projectId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving project: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{projectId}/statistics")
    public ResponseEntity<Response<ProjectStatsDTO>> getProjectStatistics(@PathVariable Long projectId) {
        try {
            Response<ProjectStatsDTO> response = projectService.getProjectStatistics(projectId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error retrieving project statistics: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Response<Void>> deleteProject(@PathVariable Long projectId) {
        try {
            Response<Void> response = projectService.deleteProject(projectId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error deleting project: " + e.getMessage(), null));
        }
    }

    // Add this method to ProjectController.java
    @PutMapping
    public ResponseEntity<Response<ProjectResponseDTO>> updateProject(@Valid @RequestBody UpdateProjectRequest request) {
        try {
            Response<ProjectResponseDTO> response = projectService.updateProject(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating project: " + e.getMessage(), null));
        }
    }
}
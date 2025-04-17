package com.event.recruitment.intelligent_recruitment_system.controller.admin;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.admin.UpdateRecruiterVerificationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.admin.RecruiterSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Get all recruiters
     * @return List of recruiters with their details
     */
    @GetMapping("/recruiters")
    public ResponseEntity<Response<List<RecruiterSummaryDTO>>> getAllRecruiters() {
        Response<List<RecruiterSummaryDTO>> response = adminService.getAllRecruiters();
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * Update recruiter verification status
     * @param updateRequest Request containing recruiter ID and new verification status
     * @return Updated recruiter data
     */
    @PutMapping("/recruiters/verification")
    public ResponseEntity<Response<?>> updateRecruiterVerificationStatus(
            @Valid @RequestBody UpdateRecruiterVerificationRequest updateRequest) {
        try {
            Response<?> response = adminService.updateRecruiterVerificationStatus(updateRequest);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new Response<>(500, "Error updating recruiter verification status: " + e.getMessage(), null));
        }
    }
}
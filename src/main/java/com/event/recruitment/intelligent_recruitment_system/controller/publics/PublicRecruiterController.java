package com.event.recruitment.intelligent_recruitment_system.controller.publics;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PublicRecruiterDTO;
import com.event.recruitment.intelligent_recruitment_system.service.public_view.PublicRecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/public/recruiters")
@RequiredArgsConstructor
public class PublicRecruiterController {

    private final PublicRecruiterService publicRecruiterService;

    @GetMapping("/{recruiterId}")
    public ResponseEntity<Response<PublicRecruiterDTO>> getRecruiterProfile(@PathVariable Long recruiterId) {
        try {
            Response<PublicRecruiterDTO> response = publicRecruiterService.getRecruiterPublicProfile(recruiterId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving recruiter profile: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{recruiterId}/portfolios")
    public ResponseEntity<Response<List<PortfolioSummaryDTO>>> getRecruiterPortfolios(@PathVariable Long recruiterId) {
        try {
            Response<List<PortfolioSummaryDTO>> response = publicRecruiterService.getRecruiterPortfolios(recruiterId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving recruiter portfolios: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{recruiterId}/portfolios/{portfolioId}")
    public ResponseEntity<Response<PortfolioResponseDTO>> getPortfolioDetails(
            @PathVariable Long recruiterId,
            @PathVariable Integer portfolioId) {
        try {
            Response<PortfolioResponseDTO> response = publicRecruiterService.getPortfolioDetails(recruiterId, portfolioId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving portfolio details: " + e.getMessage(), null));
        }
    }
}
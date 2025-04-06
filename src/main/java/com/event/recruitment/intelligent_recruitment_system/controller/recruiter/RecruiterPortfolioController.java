package com.event.recruitment.intelligent_recruitment_system.controller.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CreatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.UpdatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.EventMediaDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.service.recruiter.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/recruiters/portfolio")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterPortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<Response<PortfolioResponseDTO>> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request) {
        try {
            Response<PortfolioResponseDTO> response = portfolioService.createPortfolio(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error creating portfolio: " + e.getMessage(), null));
        }
    }

    @PutMapping
    public ResponseEntity<Response<PortfolioResponseDTO>> updatePortfolio(
            @Valid @RequestBody UpdatePortfolioRequest request) {
        try {
            Response<PortfolioResponseDTO> response = portfolioService.updatePortfolio(request);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error updating portfolio: " + e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<Response<List<PortfolioSummaryDTO>>> getAllPortfolios() {
        try {
            Response<List<PortfolioSummaryDTO>> response = portfolioService.getRecruiterPortfolios();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving portfolios: " + e.getMessage(), null));
        }
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<Response<PortfolioResponseDTO>> getPortfolioById(
            @PathVariable Integer portfolioId) {
        try {
            Response<PortfolioResponseDTO> response = portfolioService.getPortfolioById(portfolioId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error retrieving portfolio: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Response<Void>> deletePortfolio(@PathVariable Integer portfolioId) {
        try {
            Response<Void> response = portfolioService.deletePortfolio(portfolioId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting portfolio: " + e.getMessage(), null));
        }
    }

    @PostMapping(value = "/{portfolioId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<List<EventMediaDTO>>> uploadPortfolioMedia(
            @PathVariable Integer portfolioId,
            @RequestParam("files") MultipartFile[] files) {
        try {
            Response<List<EventMediaDTO>> response = portfolioService.uploadPortfolioMedia(portfolioId, files);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading media: " + e.getMessage(), null));
        }
    }

    @PostMapping(value = "/{portfolioId}/media/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<EventMediaDTO>> uploadSinglePortfolioMedia(
            @PathVariable Integer portfolioId,
            @RequestParam("file") MultipartFile file) {
        try {
            Response<EventMediaDTO> response = portfolioService.uploadSinglePortfolioMedia(portfolioId, file);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error uploading media: " + e.getMessage(), null));
        }
    }

    @DeleteMapping("/{portfolioId}/media/{mediaId}")
    public ResponseEntity<Response<Void>> deletePortfolioMedia(
            @PathVariable Integer portfolioId,
            @PathVariable Integer mediaId) {
        try {
            Response<Void> response = portfolioService.deletePortfolioMedia(portfolioId, mediaId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    new Response<>(500, "Error deleting media: " + e.getMessage(), null));
        }
    }
}
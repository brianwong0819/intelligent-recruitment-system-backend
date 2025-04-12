package com.event.recruitment.intelligent_recruitment_system.service.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CreatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.UpdatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.EventMediaDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.EventMedia;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.RecruiterPortfolio;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.EventMediaRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterPortfolioRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.PortfolioMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final RecruiterPortfolioRepository portfolioRepository;
    private final EventMediaRepository mediaRepository;
    private final RecruiterRepository recruiterRepository;
    private final SecurityUtil securityUtil;

    @Value("${file.portfolio-media-dir:C:/Users/Acer/OneDrive/Desktop/fyp/Frontend Code/event-recruitment-frontend/src/assets/portfolio-media}")
    private String portfolioMediaUploadDir;

    @Transactional
    public Response<PortfolioResponseDTO> createPortfolio(CreatePortfolioRequest request) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Create portfolio entity
            RecruiterPortfolio portfolio = PortfolioMapper.toEntity(request, recruiterId);
            RecruiterPortfolio savedPortfolio = portfolioRepository.save(portfolio);

            // Return response
            PortfolioResponseDTO response = PortfolioMapper.toDto(savedPortfolio, List.of());
            return new Response<>(HttpStatus.CREATED.value(), "Portfolio created successfully", response);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error creating portfolio: " + e.getMessage(), null);
        }
    }

    @Transactional
    public Response<PortfolioResponseDTO> updatePortfolio(UpdatePortfolioRequest request) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(request.getId(), recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            // Update portfolio
            RecruiterPortfolio portfolio = portfolioOpt.get();
            PortfolioMapper.updateEntityFromDto(portfolio, request);

            RecruiterPortfolio updatedPortfolio = portfolioRepository.save(portfolio);
            List<EventMedia> media = mediaRepository.findByEventId(updatedPortfolio.getId());

            // Return response
            PortfolioResponseDTO response = PortfolioMapper.toDto(updatedPortfolio, media);
            return new Response<>(HttpStatus.OK.value(), "Portfolio updated successfully", response);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error updating portfolio: " + e.getMessage(), null);
        }
    }

    public Response<List<PortfolioSummaryDTO>> getRecruiterPortfolios() {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Get all portfolios
            List<RecruiterPortfolio> portfolios = portfolioRepository.findByRecruiterId(recruiterId);

            // Map to DTOs with summaries
            List<PortfolioSummaryDTO> response = portfolios.stream()
                    .map(portfolio -> {
                        List<EventMedia> media = mediaRepository.findByEventId(portfolio.getId());
                        return PortfolioMapper.toSummaryDto(portfolio, media);
                    })
                    .collect(Collectors.toList());

            return new Response<>(HttpStatus.OK.value(), "Portfolios retrieved successfully", response);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving portfolios: " + e.getMessage(), null);
        }
    }

    public Response<PortfolioResponseDTO> getPortfolioById(Integer portfolioId) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            RecruiterPortfolio portfolio = portfolioOpt.get();
            List<EventMedia> media = mediaRepository.findByEventId(portfolio.getId());

            // Return response
            PortfolioResponseDTO response = PortfolioMapper.toDto(portfolio, media);
            return new Response<>(HttpStatus.OK.value(), "Portfolio retrieved successfully", response);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving portfolio: " + e.getMessage(), null);
        }
    }

    @Transactional
    public Response<Void> deletePortfolio(Integer portfolioId) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            // Delete portfolio (will cascade to media)
            portfolioRepository.delete(portfolioOpt.get());

            return new Response<>(HttpStatus.OK.value(), "Portfolio deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error deleting portfolio: " + e.getMessage(), null);
        }
    }

    @Transactional
    public Response<List<EventMediaDTO>> uploadPortfolioMedia(Integer portfolioId, MultipartFile[] files) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            // Validate file array
            if (files == null || files.length == 0) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(), "No files were uploaded", null);
            }

            List<EventMediaDTO> uploadedMedia = new ArrayList<>();

            // Process each file
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // Save file
                    String fileName = saveFile(file);
                    String mediaUrl = "/assets/portfolio-media/" + fileName;

                    // Create media entity
                    EventMedia media = PortfolioMapper.toEntity(mediaUrl, portfolioId);
                    EventMedia savedMedia = mediaRepository.save(media);

                    // Add to response list
                    uploadedMedia.add(PortfolioMapper.toDto(savedMedia));
                }
            }

            // Return response
            return new Response<>(HttpStatus.CREATED.value(),
                    uploadedMedia.size() + " media files uploaded successfully", uploadedMedia);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error uploading media: " + e.getMessage(), null);
        }
    }

    @Transactional
    public Response<Void> deletePortfolioMedia(Integer portfolioId, Integer mediaId) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            // Find media
            Optional<EventMedia> mediaOpt = mediaRepository.findByIdAndEventId(mediaId, portfolioId);

            if (mediaOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Media not found", null);
            }

            // Delete file
            try {
                String mediaUrl = mediaOpt.get().getMediaUrl();
                if (mediaUrl != null && !mediaUrl.isEmpty()) {
                    String filename = mediaUrl.substring(mediaUrl.lastIndexOf("/") + 1);
                    Path filePath = Paths.get(portfolioMediaUploadDir).resolve(filename);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                // Log but continue with DB deletion
                System.err.println("Failed to delete file: " + e.getMessage());
            }

            // Delete media from DB
            mediaRepository.delete(mediaOpt.get());

            return new Response<>(HttpStatus.OK.value(), "Media deleted successfully", null);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error deleting media: " + e.getMessage(), null);
        }
    }

    // For backward compatibility and single file uploads
    @Transactional
    public Response<EventMediaDTO> uploadSinglePortfolioMedia(Integer portfolioId, MultipartFile file) {
        try {
            // Get current recruiter
            String username = securityUtil.getCurrentUsername();
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Integer recruiterId = recruiterOpt.get().getId().intValue();

            // Find portfolio
            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterId);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            // Validate file
            if (file == null || file.isEmpty()) {
                return new Response<>(HttpStatus.BAD_REQUEST.value(), "No file was uploaded", null);
            }

            // Save file
            String fileName = saveFile(file);
            String mediaUrl = "/assets/portfolio-media/" + fileName;

            // Create media entity
            EventMedia media = PortfolioMapper.toEntity(mediaUrl, portfolioId);
            EventMedia savedMedia = mediaRepository.save(media);

            // Return response
            EventMediaDTO response = PortfolioMapper.toDto(savedMedia);
            return new Response<>(HttpStatus.CREATED.value(), "Media uploaded successfully", response);

        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error uploading media: " + e.getMessage(), null);
        }
    }

    private String saveFile(MultipartFile file) throws IOException {
        // Create directories if they don't exist
        Path uploadPath = Paths.get(portfolioMediaUploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath);

        return newFilename;
    }
}
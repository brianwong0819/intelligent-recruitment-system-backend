package com.event.recruitment.intelligent_recruitment_system.service.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.RecruiterRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.RecruiterResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.repository.location.LocationRepository;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.RecruiterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Value("${file.company-logo-dir:${file.upload-dir}/company-logos}")
    private String companyLogoUploadDir;

    @Transactional
    public Response<?> registerRecruiter(RecruiterRegistrationRequest recruiterRequest) {
        // Check if email already exists
        if (recruiterRepository.findByEmail(recruiterRequest.getEmail()).isPresent()) {
            return new Response<>(400, "Email is already registered.", null);
        }

        // Check if phone number already exists
        if (recruiterRepository.findByPhoneNumber(recruiterRequest.getPhoneNumber()).isPresent()) {
            return new Response<>(400, "Phone number is already registered.", null);
        }

        // Check if username already exists
        if (recruiterRepository.findByUsername(recruiterRequest.getUsername()).isPresent()) {
            return new Response<>(400, "Username is already taken.", null);
        }

        // Get location from database if locationId is provided
        Location location = null;
        if (recruiterRequest.getCompanyLocationId() != null) {
            Optional<Location> locationOptional = locationRepository.findById(recruiterRequest.getCompanyLocationId());
            if (locationOptional.isEmpty()) {
                return new Response<>(404, "Location with provided ID not found.", null);
            }
            location = locationOptional.get();
        }

        // Create Recruiter entity
        Recruiters recruiter = Recruiters.builder()
                .username(recruiterRequest.getUsername())
                .recruiterRepName(recruiterRequest.getRecruiterRepName())
                .email(recruiterRequest.getEmail())
                .password(passwordEncoder.encode(recruiterRequest.getPassword()))
                .phoneNumber(recruiterRequest.getPhoneNumber())
                .recruiterType(recruiterRequest.getRecruiterType() != null ?
                        recruiterRequest.getRecruiterType() : RecruiterType.INDIVIDUAL)
                .companyName(recruiterRequest.getCompanyName())
                .companyDescription(recruiterRequest.getCompanyDescription())
                .companyLocation(location)  // Use Location entity instead of string
                .companyWebsite(recruiterRequest.getCompanyWebsite())
                .verificationStatus(VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Recruiters savedRecruiter = recruiterRepository.save(recruiter);

        // Convert to DTO before returning
        RecruiterResponseDTO responseDTO = RecruiterMapper.toRecruiterResponseDTO(savedRecruiter);

        return new Response<>(201, "Recruiter registered successfully", responseDTO);
    }

    /**
     * Upload or update company logo for the logged-in recruiter
     */
    @Transactional
    public Response<?> uploadOrUpdateCompanyLogo(MultipartFile file) {
        try {
            // Get current logged-in user
            String username = securityUtil.getCurrentUsername();

            // Find recruiter by username
            Optional<Recruiters> recruiterOpt = recruiterRepository.findByUsername(username);
            if (recruiterOpt.isEmpty()) {
                return new Response<>(404, "Recruiter not found", null);
            }

            Recruiters recruiter = recruiterOpt.get();

            // Validate file
            if (file.isEmpty()) {
                return new Response<>(400, "Please upload a company logo", null);
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new Response<>(400, "Only image files are allowed", null);
            }

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(companyLogoUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // If recruiter already has a company logo, delete the old file
            if (recruiter.getCompanyLogoUrl() != null && !recruiter.getCompanyLogoUrl().isEmpty()
                    && !recruiter.getCompanyLogoUrl().startsWith("http")) {
                String oldFilename = recruiter.getCompanyLogoUrl().substring(recruiter.getCompanyLogoUrl().lastIndexOf("/") + 1);
                Path oldFilePath = Paths.get(companyLogoUploadDir).resolve(oldFilename);
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    // Log the error but continue with update
                    System.err.println("Failed to delete old company logo file: " + e.getMessage());
                }
            }

            // Generate a unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID() + fileExtension;

            // Save the new file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Update recruiter's company logo URL
            String companyLogoUrl = "/api/files/company-logos/" + newFilename;
            recruiter.setCompanyLogoUrl(companyLogoUrl);
            recruiterRepository.save(recruiter);

            // Convert to DTO before returning
            RecruiterResponseDTO responseDTO = RecruiterMapper.toRecruiterResponseDTO(recruiter);
            return new Response<>(200, "Company logo updated successfully", responseDTO);

        } catch (IOException e) {
            return new Response<>(500, "Failed to upload company logo: " + e.getMessage(), null);
        } catch (Exception e) {
            return new Response<>(500, "An error occurred: " + e.getMessage(), null);
        }
    }
}
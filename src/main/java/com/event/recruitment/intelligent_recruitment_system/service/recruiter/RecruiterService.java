package com.event.recruitment.intelligent_recruitment_system.service.recruiter;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.RecruiterRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.RecruiterResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.util.RecruiterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;

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
                .companyLocation(recruiterRequest.getCompanyLocation())
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
}
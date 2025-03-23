package com.event.recruitment.intelligent_recruitment_system.service;

import com.event.recruitment.intelligent_recruitment_system.dto.RecruiterRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.model.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.model.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.VerificationStatus;
import com.event.recruitment.intelligent_recruitment_system.repository.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public RecruiterService(RecruiterRepository recruiterRepository, PasswordEncoder passwordEncoder) {
        this.recruiterRepository = recruiterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Response<Recruiters> registerRecruiter(RecruiterRegistrationRequest recruiterRequest) {
        Optional<Recruiters> existingRecruiter = recruiterRepository.findByEmail(recruiterRequest.getEmail());
        if (existingRecruiter.isPresent()) {
            return Response.<Recruiters>builder()
                    .statusCode(400)
                    .message("Email is already registered.")
                    .data(null)
                    .build();
        }

        // Check if phone number already exists
        Optional<Recruiters> existingRecruiterByPhone = recruiterRepository.findByPhoneNumber(recruiterRequest.getPhoneNumber());
        if (existingRecruiterByPhone.isPresent()) {
            return Response.<Recruiters>builder()
                    .statusCode(400)
                    .message("Phone number is already registered.")
                    .data(null)
                    .build();
        }

        // Encrypt password
        String encryptedPassword = passwordEncoder.encode(recruiterRequest.getPassword());

        // Create Recruiter entity
        Recruiters recruiter = Recruiters.builder()
                .username(recruiterRequest.getUsername())
                .recruiterRepName(recruiterRequest.getRecruiterRepName())
                .email(recruiterRequest.getEmail())
                .password(encryptedPassword)
                .phoneNumber(recruiterRequest.getPhoneNumber())
                .recruiterType(recruiterRequest.getRecruiterType() != null ? recruiterRequest.getRecruiterType() : RecruiterType.INDIVIDUAL)
                .companyName(recruiterRequest.getCompanyName())
                .companyLogoUrl(recruiterRequest.getCompanyLogoUrl())
                .companyDescription(recruiterRequest.getCompanyDescription())
                .companyLocation(recruiterRequest.getCompanyLocation())
                .companyWebsite(recruiterRequest.getCompanyWebsite())
                .verificationStatus(recruiterRequest.getVerificationStatus() != null ? recruiterRequest.getVerificationStatus() : VerificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        recruiterRepository.save(recruiter);

        return Response.<Recruiters>builder()
                .statusCode(201)
                .message("Recruiter registered successfully")
                .data(recruiter)
                .build();
    }
}

package com.event.recruitment.intelligent_recruitment_system.service;

import com.event.recruitment.intelligent_recruitment_system.dto.CandidateRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.model.Candidates;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CandidateService(CandidateRepository candidateRepository, PasswordEncoder passwordEncoder) {
        this.candidateRepository = candidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register candidate
    @Transactional
    public Response<Candidates> registerCandidate(CandidateRegistrationRequest candidateRequest) {
        Optional<Candidates> existingCandidate = candidateRepository.findByEmail(candidateRequest.getEmail());
        if (existingCandidate.isPresent()) {
            return Response.<Candidates>builder()
                    .statusCode(400)
                    .message("Email is already registered.")
                    .data(null)
                    .build();
        }

        Optional<Candidates> existingCandidateByPhone = candidateRepository.findByPhoneNumber(candidateRequest.getPhoneNumber());
        if (existingCandidateByPhone.isPresent()) {
            return Response.<Candidates>builder()
                    .statusCode(400)
                    .message("Phone number is already registered.")
                    .data(null)
                    .build();
        }

        String encryptedPassword = passwordEncoder.encode(candidateRequest.getPassword());

        Candidates candidates = Candidates.builder()
                .username(candidateRequest.getUsername())
                .name(candidateRequest.getName())
                .email(candidateRequest.getEmail())
                .password(encryptedPassword)
                .phoneNumber(candidateRequest.getPhoneNumber())
                .gender(candidateRequest.getGender())
                .dateOfBirth(candidateRequest.getDateOfBirth())
                .race(candidateRequest.getRace())
                .profilePictureUrl(candidateRequest.getProfilePictureUrl())
                .createdAt(LocalDateTime.now())
                .build();

        candidateRepository.save(candidates);

        return Response.<Candidates>builder()
                .statusCode(201)
                .message("Candidate registered successfully")
                .data(candidates)
                .build();
    }

    // Update candidate information
    @Transactional
    public Response<Candidates> updateCandidate(Long id, CandidateRegistrationRequest candidateRegistrationRequest) {
        Candidates candidates = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        candidates.setUsername(candidateRegistrationRequest.getUsername());
        candidates.setName(candidateRegistrationRequest.getName());
        candidates.setEmail(candidateRegistrationRequest.getEmail());
        candidates.setPhoneNumber(candidateRegistrationRequest.getPhoneNumber());
        candidates.setGender(candidateRegistrationRequest.getGender());
        candidates.setDateOfBirth(candidateRegistrationRequest.getDateOfBirth());
        candidates.setRace(candidateRegistrationRequest.getRace());
        candidates.setProfilePictureUrl(candidateRegistrationRequest.getProfilePictureUrl());

        candidateRepository.save(candidates);

        return Response.<Candidates>builder()
                .statusCode(200)
                .message("Candidate updated successfully")
                .data(candidates)
                .build();
    }

    // Get current logged-in candidate's profile
    public Response<Candidates> getCandidateProfile() {
        // Get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();  // Get username from authentication context

        Optional<Candidates> candidate = candidateRepository.findByUsername(username);

        if (candidate.isEmpty()) {
            return Response.<Candidates>builder()
                    .statusCode(404)
                    .message("Candidate not found")
                    .data(null)
                    .build();
        }

        return Response.<Candidates>builder()
                .statusCode(200)
                .message("Candidate profile fetched successfully")
                .data(candidate.get())
                .build();
    }
}
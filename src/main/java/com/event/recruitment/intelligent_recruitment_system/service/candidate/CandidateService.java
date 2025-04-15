package com.event.recruitment.intelligent_recruitment_system.service.candidate;

import com.event.recruitment.intelligent_recruitment_system.dto.request.auth.CandidateRegistrationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.candidate.UpdateAvailabilityRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.AvailabilityResponse;
import com.event.recruitment.intelligent_recruitment_system.dto.response.candidate.CandidateResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateReputation;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.CandidateAvailabilityDate;
import com.event.recruitment.intelligent_recruitment_system.model.enums.Availability;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateAvailabilityDateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateReputationRepository;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import com.event.recruitment.intelligent_recruitment_system.util.CandidateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final PasswordEncoder passwordEncoder;
    private final CandidateAvailabilityDateRepository availabilityDateRepository;
    private final SecurityUtil securityUtil;
    private final CandidateReputationRepository candidateReputationRepository;


    // Register candidate
    @Transactional
    public Response<?> registerCandidate(CandidateRegistrationRequest request) {
        // Check if email already exists
        if (candidateRepository.findByEmail(request.getEmail()).isPresent()) {
            return new Response<>(400, "Email is already registered.", null);
        }

        // Check if phone number already exists
        if (request.getPhoneNumber() != null &&
                candidateRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new Response<>(400, "Phone number is already registered.", null);
        }

        // Check if username already exists
        if (candidateRepository.findByUsername(request.getUsername()).isPresent()) {
            return new Response<>(400, "Username is already taken.", null);
        }

        // Validate languages field
        if (request.getLanguages() == null || request.getLanguages().isEmpty()) {
            return new Response<>(400, "At least one language must be selected.", null);
        }

        // Build the candidate entity
        Candidates candidate = Candidates.builder()
                .username(request.getUsername())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .race(request.getRace())
                .employmentStatus(request.getEmploymentStatus())
                .profilePictureUrl(request.getProfilePictureUrl())
                .bio(request.getBio())  // Set bio field
                .languages(request.getLanguages())  // Set languages field
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build();

        Candidates savedCandidate = candidateRepository.save(candidate);

        CandidateReputation reputation = CandidateReputation.builder()
                .candidateId(savedCandidate.getId())
                .score(100.0) // 初始分数为100
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        candidateReputationRepository.save(reputation);

        // Convert to DTO before returning
        CandidateResponseDTO responseDTO = CandidateMapper.toCandidateResponseDTO(savedCandidate);

        return new Response<>(201, "Candidate registered successfully", responseDTO);
    }

    // Update candidate information
    @Transactional
    public Response<?> updateCandidate(Long id, CandidateRegistrationRequest candidateRequest) {
        Candidates candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        // Check email uniqueness if it's being changed
        if (!candidate.getEmail().equals(candidateRequest.getEmail()) &&
                candidateRepository.findByEmail(candidateRequest.getEmail()).isPresent()) {
            return new Response<>(400, "Email is already registered by another account.", null);
        }

        // Check phone uniqueness if it's being changed
        if (candidateRequest.getPhoneNumber() != null &&
                !candidateRequest.getPhoneNumber().equals(candidate.getPhoneNumber()) &&
                candidateRepository.findByPhoneNumber(candidateRequest.getPhoneNumber()).isPresent()) {
            return new Response<>(400, "Phone number is already registered by another account.", null);
        }

        candidate.setUsername(candidateRequest.getUsername());
        candidate.setName(candidateRequest.getName());
        candidate.setEmail(candidateRequest.getEmail());
        candidate.setPhoneNumber(candidateRequest.getPhoneNumber());
        candidate.setGender(candidateRequest.getGender());
        candidate.setDateOfBirth(candidateRequest.getDateOfBirth());
        candidate.setRace(candidateRequest.getRace());
        candidate.setEmploymentStatus(candidateRequest.getEmploymentStatus()); // Added employment status update
        candidate.setProfilePictureUrl(candidateRequest.getProfilePictureUrl());

        candidateRepository.save(candidate);

        CandidateResponseDTO responseDTO = CandidateMapper.toCandidateResponseDTO(candidate);

        return new Response<>(200, "Candidate updated successfully", responseDTO);
    }

    // Get current logged-in candidate's profile
    public Response<?> getCandidateProfile() {
        String username = securityUtil.getCurrentUsername();
        Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

        if (candidateOpt.isEmpty()) {
            return new Response<>(404, "Candidate not found", null);
        }

        CandidateResponseDTO responseDTO = CandidateMapper.toCandidateResponseDTO(candidateOpt.get());

        return new Response<>(200, "Candidate profile fetched successfully", responseDTO);
    }

    @Transactional
    public Response<?> updateAvailability(UpdateAvailabilityRequest request) {
        String username = securityUtil.getCurrentUsername();
        Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

        if (candidateOpt.isEmpty()) {
            return new Response<>(404, "Candidate not found", null);
        }

        Candidates candidate = candidateOpt.get();
        Availability availabilityType = request.getAvailabilityType();

        // Update candidate availability type - pass the enum directly
        candidate.setAvailability(availabilityType);
        candidateRepository.save(candidate);

        // If CUSTOM_DATES, save the custom dates
        if (availabilityType == Availability.CUSTOM_DATES) {
            if (request.getCustomDates() == null || request.getCustomDates().isEmpty()) {
                return new Response<>(400, "Custom dates are required when availability type is CUSTOM_DATES", null);
            }

            // Delete existing availability dates
            availabilityDateRepository.deleteByCandidateId(candidate.getId());

            // Save new availability dates
            List<CandidateAvailabilityDate> availabilityDates = new ArrayList<>();
            for (LocalDate date : request.getCustomDates()) {
                CandidateAvailabilityDate availabilityDate = new CandidateAvailabilityDate();
                availabilityDate.setCandidateId(candidate.getId());
                availabilityDate.setAvailableDate(date);
                availabilityDates.add(availabilityDate);
            }

            availabilityDateRepository.saveAll(availabilityDates);
        } else {
            // If not CUSTOM_DATES, delete any existing custom dates
            availabilityDateRepository.deleteByCandidateId(candidate.getId());
        }

        AvailabilityResponse availabilityResponse = new AvailabilityResponse();
        availabilityResponse.setAvailabilityType(availabilityType);
        availabilityResponse.setCustomDates(availabilityType == Availability.CUSTOM_DATES ?
                request.getCustomDates() : new ArrayList<>());

        return new Response<>(200, "Availability updated successfully", availabilityResponse);
    }

    public Response<?> getAvailability() {
        String username = securityUtil.getCurrentUsername();
        Optional<Candidates> candidateOpt = candidateRepository.findByUsername(username);

        if (candidateOpt.isEmpty()) {
            return new Response<>(404, "Candidate not found", null);
        }

        Candidates candidate = candidateOpt.get();

        // Handle case where availability is null
        Availability availabilityType = candidate.getAvailability();
        if (availabilityType == null) {
            availabilityType = Availability.ANYTIME;
        }

        List<LocalDate> customDates = new ArrayList<>();
        if (availabilityType == Availability.CUSTOM_DATES) {
            customDates = availabilityDateRepository.findByCandidateId(candidate.getId())
                    .stream()
                    .map(CandidateAvailabilityDate::getAvailableDate)
                    .collect(Collectors.toList());
        }

        AvailabilityResponse response = new AvailabilityResponse();
        response.setAvailabilityType(availabilityType);
        response.setCustomDates(customDates);

        return new Response<>(200, "Availability retrieved successfully", response);
    }
}
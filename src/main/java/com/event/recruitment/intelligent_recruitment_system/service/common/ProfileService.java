package com.event.recruitment.intelligent_recruitment_system.service.common;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.common.ChangePasswordRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.common.UpdateProfileRequest;
import com.event.recruitment.intelligent_recruitment_system.model.entity.candidate.Candidates;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.candidate.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.jwt.JwtUtil;
import com.event.recruitment.intelligent_recruitment_system.security.util.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final RecruiterRepository recruiterRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    public ProfileService(CandidateRepository candidateRepository,
                          RecruiterRepository recruiterRepository,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder,
                          SecurityUtil securityUtil) {
        this.candidateRepository = candidateRepository;
        this.recruiterRepository = recruiterRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.securityUtil = securityUtil;
    }

    // Get current user's profile
    public Response<Object> getProfile() {
        String token = jwtUtil.getTokenFromSecurityContext();
        if (token == null) {
            return Response.builder()
                    .statusCode(401)
                    .message("Unauthorized: No authentication found")
                    .data(null)
                    .build();
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        if (role == null) {
            return Response.builder()
                    .statusCode(400)
                    .message("Invalid JWT: Role not found")
                    .data(null)
                    .build();
        }

        return getProfileByRole(role, username);
    }

    private Response<Object> getProfileByRole(String role, String username) {
        if ("CANDIDATE".equalsIgnoreCase(role)) {
            return fetchCandidateProfile(username);
        } else if ("RECRUITER".equalsIgnoreCase(role)) {
            return fetchRecruiterProfile(username);
        }
        return Response.builder()
                .statusCode(400)
                .message("Invalid role")
                .data(null)
                .build();
    }

    private Response<Object> fetchCandidateProfile(String username) {
        Optional<Candidates> candidate = candidateRepository.findByUsername(username);
        if (candidate.isPresent()) {
            return Response.builder()
                    .statusCode(200)
                    .message("Candidate profile fetched successfully")
                    .data(candidate.get())
                    .build();
        }
        return Response.builder()
                .statusCode(404)
                .message("Candidate not found")
                .data(null)
                .build();
    }

    private Response<Object> fetchRecruiterProfile(String username) {
        Optional<Recruiters> recruiter = recruiterRepository.findByUsername(username);
        if (recruiter.isPresent()) {
            return Response.builder()
                    .statusCode(200)
                    .message("Recruiter profile fetched successfully")
                    .data(recruiter.get())
                    .build();
        }
        return Response.builder()
                .statusCode(404)
                .message("Recruiter not found")
                .data(null)
                .build();
    }

    // Update profile
    public Response<Object> updateProfile(UpdateProfileRequest updateRequest) {
        String token = jwtUtil.getTokenFromSecurityContext();
        if (token == null) {
            return Response.builder().statusCode(401).message("Unauthorized: No authentication found").data(null).build();
        }

        String username = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractRole(token);

        if ("CANDIDATE".equalsIgnoreCase(role)) {
            return updateCandidateProfile(updateRequest, username);
        } else if ("RECRUITER".equalsIgnoreCase(role)) {
            return updateRecruiterProfile(updateRequest, username);
        }

        return Response.builder().statusCode(400).message("Invalid role").data(null).build();
    }

    private Response<Object> updateCandidateProfile(UpdateProfileRequest updateRequest, String username) {
        Optional<Candidates> optionalCandidate = candidateRepository.findByUsername(username);
        if (optionalCandidate.isPresent()) {
            Candidates candidate = optionalCandidate.get();

            // Check for email existence if email is being updated
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(candidate.getEmail())) {
                // Check if email exists in candidate repository
                Optional<Candidates> emailExistsCandidate = candidateRepository.findByEmail(updateRequest.getEmail());
                // Check if email exists in recruiter repository
                Optional<Recruiters> emailExistsRecruiter = recruiterRepository.findByEmail(updateRequest.getEmail());

                if (emailExistsCandidate.isPresent() || emailExistsRecruiter.isPresent()) {
                    return Response.builder()
                            .statusCode(409)
                            .message("Email already exists")
                            .data(null)
                            .build();
                }
                candidate.setEmail(updateRequest.getEmail());
            }

            // Update other fields
            if (updateRequest.getName() != null) candidate.setName(updateRequest.getName());
            if (updateRequest.getPhoneNumber() != null) candidate.setPhoneNumber(updateRequest.getPhoneNumber());
            if (updateRequest.getGender() != null) candidate.setGender(updateRequest.getGender());
            if (updateRequest.getDateOfBirth() != null) candidate.setDateOfBirth(updateRequest.getDateOfBirth());
            if (updateRequest.getProfilePictureUrl() != null) candidate.setProfilePictureUrl(updateRequest.getProfilePictureUrl());
            if (updateRequest.getPreferredLocation() != null) candidate.setPreferredLocation(updateRequest.getPreferredLocation());
            if (updateRequest.getAvailability() != null) candidate.setAvailability(updateRequest.getAvailability());
            if (updateRequest.getBio() != null) candidate.setBio(updateRequest.getBio());
            if (updateRequest.getResumeUrl() != null) candidate.setResumeUrl(updateRequest.getResumeUrl());

            // Update Candidate specific fields
            if (updateRequest.getLanguages() != null) {
                candidate.setLanguages(updateRequest.getLanguages());
                candidate.setEmploymentStatus(updateRequest.getEmploymentStatus());
                candidate.setRace(updateRequest.getRace());
            }

            candidateRepository.save(candidate);
            return Response.builder()
                    .statusCode(200)
                    .message("Candidate profile updated successfully")
                    .data(candidate)
                    .build();
        }
        return Response.builder()
                .statusCode(404)
                .message("Candidate not found")
                .data(null)
                .build();
    }

    private Response<Object> updateRecruiterProfile(UpdateProfileRequest updateRequest, String username) {
        Optional<Recruiters> optionalRecruiter = recruiterRepository.findByUsername(username);
        if (optionalRecruiter.isPresent()) {
            Recruiters recruiter = optionalRecruiter.get();

            // Check for email existence if email is being updated
            if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(recruiter.getEmail())) {
                // Check if email exists in candidate repository
                Optional<Candidates> emailExistsCandidate = candidateRepository.findByEmail(updateRequest.getEmail());
                // Check if email exists in recruiter repository
                Optional<Recruiters> emailExistsRecruiter = recruiterRepository.findByEmail(updateRequest.getEmail());

                if (emailExistsCandidate.isPresent() || emailExistsRecruiter.isPresent()) {
                    return Response.builder()
                            .statusCode(409)
                            .message("Email already exists")
                            .data(null)
                            .build();
                }
                recruiter.setEmail(updateRequest.getEmail());
            }

            // Update other fields
            if (updateRequest.getName() != null) recruiter.setRecruiterRepName(updateRequest.getName());
            if (updateRequest.getPhoneNumber() != null) recruiter.setPhoneNumber(updateRequest.getPhoneNumber());
            if (updateRequest.getRecruiterType() != null) recruiter.setRecruiterType(updateRequest.getRecruiterType());
            if (updateRequest.getCompanyName() != null) recruiter.setCompanyName(updateRequest.getCompanyName());
            if (updateRequest.getCompanyLogoUrl() != null) recruiter.setCompanyLogoUrl(updateRequest.getCompanyLogoUrl());
            if (updateRequest.getCompanyDescription() != null) recruiter.setCompanyDescription(updateRequest.getCompanyDescription());
            if (updateRequest.getCompanyLocation() != null) recruiter.setCompanyLocation(updateRequest.getCompanyLocation());
            if (updateRequest.getCompanyWebsite() != null) recruiter.setCompanyWebsite(updateRequest.getCompanyWebsite());

            recruiterRepository.save(recruiter);
            return Response.builder()
                    .statusCode(200)
                    .message("Recruiter profile updated successfully")
                    .data(recruiter)
                    .build();
        }
        return Response.builder()
                .statusCode(404)
                .message("Recruiter not found")
                .data(null)
                .build();
    }

    // Change password
    @Transactional
    public Response<?> changePassword(ChangePasswordRequest request) {
        String username = securityUtil.getCurrentUsername();
        if (username == null) {
            return new Response<>(401, "Unauthorized: No authentication found", null);
        }

        // Get user role to determine which repository to use
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isCandidate = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CANDIDATE"));
        boolean isRecruiter = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RECRUITER"));

        if (isCandidate) {
            return changeCandidatePassword(request, username);
        } else if (isRecruiter) {
            return changeRecruiterPassword(request, username);
        }

        return new Response<>(400, "Invalid user role", null);
    }

    private Response<?> changeCandidatePassword(ChangePasswordRequest request, String username) {
        Optional<Candidates> optionalCandidate = candidateRepository.findByUsername(username);
        if (optionalCandidate.isEmpty()) {
            return new Response<>(404, "Candidate not found", null);
        }

        Candidates candidate = optionalCandidate.get();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), candidate.getPassword())) {
            return new Response<>(401, "Current password is incorrect", null);
        }

        // Update password
        candidate.setPassword(passwordEncoder.encode(request.getNewPassword()));
        candidateRepository.save(candidate);

        return new Response<>(200, "Password changed successfully", null);
    }

    private Response<?> changeRecruiterPassword(ChangePasswordRequest request, String username) {
        Optional<Recruiters> optionalRecruiter = recruiterRepository.findByUsername(username);
        if (optionalRecruiter.isEmpty()) {
            return new Response<>(404, "Recruiter not found", null);
        }

        Recruiters recruiter = optionalRecruiter.get();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), recruiter.getPassword())) {
            return new Response<>(401, "Current password is incorrect", null);
        }

        // Update password
        recruiter.setPassword(passwordEncoder.encode(request.getNewPassword()));
        recruiterRepository.save(recruiter);

        return new Response<>(200, "Password changed successfully", null);
    }
}
package com.event.recruitment.intelligent_recruitment_system.service;

import com.event.recruitment.intelligent_recruitment_system.dto.UpdateProfileRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.Response;
import com.event.recruitment.intelligent_recruitment_system.model.*;
import com.event.recruitment.intelligent_recruitment_system.repository.CandidateRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileService {

    private final CandidateRepository candidateRepository;
    private final RecruiterRepository recruiterRepository;
    private final JwtUtil jwtUtil;

    public ProfileService(CandidateRepository candidateRepository,
                          RecruiterRepository recruiterRepository,
                          JwtUtil jwtUtil) {
        this.candidateRepository = candidateRepository;
        this.recruiterRepository = recruiterRepository;
        this.jwtUtil = jwtUtil;
    }

    // 获取当前用户的个人资料
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

    // 更新 Profile
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

            // 更新通用字段
            if (updateRequest.getName() != null) candidate.setName(updateRequest.getName());
            if (updateRequest.getEmail() != null) candidate.setEmail(updateRequest.getEmail());
            if (updateRequest.getPhoneNumber() != null) candidate.setPhoneNumber(updateRequest.getPhoneNumber());
            if (updateRequest.getGender() != null) candidate.setGender(updateRequest.getGender());
            if (updateRequest.getDateOfBirth() != null) candidate.setDateOfBirth(updateRequest.getDateOfBirth());
            if (updateRequest.getProfilePictureUrl() != null) candidate.setProfilePictureUrl(updateRequest.getProfilePictureUrl());
            if (updateRequest.getPreferredLocation() != null) candidate.setPreferredLocation(updateRequest.getPreferredLocation());
            if (updateRequest.getAvailability() != null) candidate.setAvailability(updateRequest.getAvailability());
            if (updateRequest.getBio() != null) candidate.setBio(updateRequest.getBio());
            if (updateRequest.getResumeUrl() != null) candidate.setResumeUrl(updateRequest.getResumeUrl());

            // 更新 Candidate 特有字段
            if (updateRequest.getLanguages() != null) {
                candidate.setLanguages(updateRequest.getLanguages());
            }

            candidateRepository.save(candidate);
            return Response.builder().statusCode(200).message("Candidate profile updated successfully").data(candidate).build();
        }
        return Response.builder().statusCode(404).message("Candidate not found").data(null).build();
    }

    private Response<Object> updateRecruiterProfile(UpdateProfileRequest updateRequest, String username) {
        Optional<Recruiters> optionalRecruiter = recruiterRepository.findByUsername(username);
        if (optionalRecruiter.isPresent()) {
            Recruiters recruiter = optionalRecruiter.get();

            // 更新通用字段
            if (updateRequest.getName() != null) recruiter.setRecruiterRepName(updateRequest.getName());
            if (updateRequest.getEmail() != null) recruiter.setEmail(updateRequest.getEmail());
            if (updateRequest.getPhoneNumber() != null) recruiter.setPhoneNumber(updateRequest.getPhoneNumber());
            if (updateRequest.getRecruiterType() != null) recruiter.setRecruiterType(updateRequest.getRecruiterType());
            if (updateRequest.getCompanyName() != null) recruiter.setCompanyName(updateRequest.getCompanyName());
            if (updateRequest.getCompanyLogoUrl() != null) recruiter.setCompanyLogoUrl(updateRequest.getCompanyLogoUrl());
            if (updateRequest.getCompanyDescription() != null) recruiter.setCompanyDescription(updateRequest.getCompanyDescription());
            if (updateRequest.getCompanyLocation() != null) recruiter.setCompanyLocation(updateRequest.getCompanyLocation());
            if (updateRequest.getCompanyWebsite() != null) recruiter.setCompanyWebsite(updateRequest.getCompanyWebsite());

            recruiterRepository.save(recruiter);
            return Response.builder().statusCode(200).message("Recruiter profile updated successfully").data(recruiter).build();
        }
        return Response.builder().statusCode(404).message("Recruiter not found").data(null).build();
    }
}

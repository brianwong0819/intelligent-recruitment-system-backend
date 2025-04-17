package com.event.recruitment.intelligent_recruitment_system.service.admin;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.request.admin.UpdateRecruiterVerificationRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.admin.RecruiterSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.util.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RecruiterRepository recruiterRepository;
    private final AdminMapper adminMapper;

    /**
     * Get all recruiters with pagination and sorting options
     * @return Response containing list of recruiter summary DTOs
     */
    public Response<List<RecruiterSummaryDTO>> getAllRecruiters() {
        List<Recruiters> recruiters = recruiterRepository.findByIsDeletedFalse();

        List<RecruiterSummaryDTO> recruiterSummaries = recruiters.stream()
                .map(adminMapper::mapToRecruiterSummaryDTO)
                .collect(Collectors.toList());

        return new Response<>(HttpStatus.OK.value(), "Successfully retrieved all recruiters", recruiterSummaries);
    }

    /**
     * Update recruiter verification status
     * @param request The update request containing recruiter ID and new verification status
     * @return Response containing updated recruiter summary
     */
    @Transactional
    public Response<?> updateRecruiterVerificationStatus(UpdateRecruiterVerificationRequest request) {
        Optional<Recruiters> recruiterOpt = recruiterRepository.findById(request.getRecruiterId());

        if (recruiterOpt.isEmpty()) {
            return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
        }

        Recruiters recruiter = recruiterOpt.get();
        recruiter.setVerificationStatus(request.getVerificationStatus());

        Recruiters updatedRecruiter = recruiterRepository.save(recruiter);
        RecruiterSummaryDTO recruiterSummaryDTO = adminMapper.mapToRecruiterSummaryDTO(updatedRecruiter);

        return new Response<>(HttpStatus.OK.value(), "Successfully updated recruiter verification status", recruiterSummaryDTO);
    }
}
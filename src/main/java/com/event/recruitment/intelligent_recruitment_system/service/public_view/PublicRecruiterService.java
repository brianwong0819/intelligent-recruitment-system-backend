package com.event.recruitment.intelligent_recruitment_system.service.public_view;

import com.event.recruitment.intelligent_recruitment_system.dto.common.Response;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.EventMediaDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PublicRecruiterDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.EventMedia;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.RecruiterPortfolio;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.Recruiters;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.EventMediaRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterPortfolioRepository;
import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import com.event.recruitment.intelligent_recruitment_system.util.PortfolioMapper;
import com.event.recruitment.intelligent_recruitment_system.util.RecruiterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicRecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final RecruiterPortfolioRepository portfolioRepository;
    private final EventMediaRepository mediaRepository;

    public Response<PublicRecruiterDTO> getRecruiterPublicProfile(Long recruiterId) {
        try {
            Optional<Recruiters> recruiterOpt = recruiterRepository.findById(recruiterId);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            Recruiters recruiter = recruiterOpt.get();
            PublicRecruiterDTO dto = RecruiterMapper.toPublicRecruiterDTO(recruiter);

            return new Response<>(HttpStatus.OK.value(), "Recruiter profile retrieved successfully", dto);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving recruiter profile: " + e.getMessage(), null);
        }
    }

    public Response<List<PortfolioSummaryDTO>> getRecruiterPortfolios(Long recruiterId) {
        try {
            Optional<Recruiters> recruiterOpt = recruiterRepository.findById(recruiterId);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            // Convert Long to Integer for repository call (due to the entity using Integer)
            Integer recruiterIdInt = recruiterId.intValue();

            List<RecruiterPortfolio> portfolios = portfolioRepository.findByRecruiterId(recruiterIdInt);

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

    public Response<PortfolioResponseDTO> getPortfolioDetails(Long recruiterId, Integer portfolioId) {
        try {
            Optional<Recruiters> recruiterOpt = recruiterRepository.findById(recruiterId);

            if (recruiterOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Recruiter not found", null);
            }

            // Convert Long to Integer for repository call
            Integer recruiterIdInt = recruiterId.intValue();

            Optional<RecruiterPortfolio> portfolioOpt =
                    portfolioRepository.findByIdAndRecruiterId(portfolioId, recruiterIdInt);

            if (portfolioOpt.isEmpty()) {
                return new Response<>(HttpStatus.NOT_FOUND.value(), "Portfolio not found", null);
            }

            RecruiterPortfolio portfolio = portfolioOpt.get();
            List<EventMedia> media = mediaRepository.findByEventId(portfolio.getId());

            // Map to DTO
            PortfolioResponseDTO response = PortfolioMapper.toDto(portfolio, media);
            return new Response<>(HttpStatus.OK.value(), "Portfolio retrieved successfully", response);
        } catch (Exception e) {
            return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error retrieving portfolio: " + e.getMessage(), null);
        }
    }
}
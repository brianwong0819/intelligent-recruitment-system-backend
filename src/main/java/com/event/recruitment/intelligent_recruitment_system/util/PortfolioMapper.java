package com.event.recruitment.intelligent_recruitment_system.util;

import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.CreatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.request.recruiter.UpdatePortfolioRequest;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.EventMediaDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioResponseDTO;
import com.event.recruitment.intelligent_recruitment_system.dto.response.recruiter.PortfolioSummaryDTO;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.EventMedia;
import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.RecruiterPortfolio;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PortfolioMapper {

    public static RecruiterPortfolio toEntity(CreatePortfolioRequest request, Integer recruiterId) {
        return RecruiterPortfolio.builder()
                .recruiterId(recruiterId)
                .eventName(request.getEventName())
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .eventDescription(request.getEventDescription())
                .build();
    }

    public static EventMedia toEntity(String mediaUrl, Integer eventId) {
        return EventMedia.builder()
                .eventId(eventId)
                .mediaUrl(mediaUrl)
                .build();
    }

    public static void updateEntityFromDto(RecruiterPortfolio portfolio, UpdatePortfolioRequest request) {
        if (request.getEventName() != null) {
            portfolio.setEventName(request.getEventName());
        }

        if (request.getEventStartDate() != null) {
            portfolio.setEventStartDate(request.getEventStartDate());
        }

        if (request.getEventEndDate() != null) {
            portfolio.setEventEndDate(request.getEventEndDate());
        }

        if (request.getEventDescription() != null) {
            portfolio.setEventDescription(request.getEventDescription());
        }
    }

    public static PortfolioResponseDTO toDto(RecruiterPortfolio portfolio, List<EventMedia> media) {
        List<EventMediaDTO> mediaDtos = media != null ?
                media.stream()
                        .map(PortfolioMapper::toDto)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return PortfolioResponseDTO.builder()
                .id(portfolio.getId())
                .eventName(portfolio.getEventName())
                .eventStartDate(portfolio.getEventStartDate())
                .eventEndDate(portfolio.getEventEndDate())
                .eventDescription(portfolio.getEventDescription())
                .uploadedAt(portfolio.getUploadedAt())
                .eventMedia(mediaDtos)
                .build();
    }

    public static EventMediaDTO toDto(EventMedia media) {
        return EventMediaDTO.builder()
                .id(media.getId())
                .mediaUrl(media.getMediaUrl())
                .uploadedAt(media.getUploadedAt())
                .build();
    }

    public static PortfolioSummaryDTO toSummaryDto(RecruiterPortfolio portfolio, List<EventMedia> media) {
        String coverImageUrl = media != null && !media.isEmpty() ?
                media.get(0).getMediaUrl() : null;

        return PortfolioSummaryDTO.builder()
                .id(portfolio.getId())
                .eventName(portfolio.getEventName())
                .eventStartDate(portfolio.getEventStartDate())
                .eventEndDate(portfolio.getEventEndDate())
                .eventDescription(portfolio.getEventDescription())
                .uploadedAt(portfolio.getUploadedAt())
                .coverImageUrl(coverImageUrl)
                .mediaCount(media != null ? media.size() : 0)
                .build();
    }
}
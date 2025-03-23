package com.event.recruitment.intelligent_recruitment_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CandidateResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String name;
    private String phoneNumber;
    private String gender;
    private String race;
    private String profilePictureUrl;
    private String preferredLocation;
    private String bio;
    private String[] languages; // 可用语言
    private String resumeUrl;
    private Boolean isDeleted;
}

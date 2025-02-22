package com.event.recruitment.intelligent_recruitment_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String name;
    private String email;
    private String password;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;  // Enum type for gender

    private String dateOfBirth;
    private String nationality;
    private String profilePictureUrl;
    private String preferredLocation;

    @Enumerated(EnumType.STRING)
    private Availability availability;  // Enum type for availability

    private String bio;

    @ElementCollection(targetClass = Language.class)
    @Enumerated(EnumType.STRING)
    private List<Language> languages;

    private String resumeUrl;

    private Boolean isDeleted;

    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}

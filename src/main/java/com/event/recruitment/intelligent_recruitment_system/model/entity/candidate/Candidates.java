package com.event.recruitment.intelligent_recruitment_system.model.entity.candidate;

import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "candidates")
public class Candidates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Race race;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "preferred_location_id")
    private Location preferredLocation;

    @Enumerated(EnumType.STRING)
    private Availability availability;

    @Enumerated(EnumType.STRING)
    private EmploymentStatus employmentStatus;

    private String bio;

    @ElementCollection(targetClass = Language.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "candidate_languages", joinColumns = @JoinColumn(name = "candidate_id"))
    @Column(name = "languages")
    private List<Language> languages;

    @Column(name = "resume_url")
    private String resumeUrl;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // New field for searchable status
    @Column(name = "is_searchable")
    @Builder.Default
    private Boolean isSearchable = false;
}
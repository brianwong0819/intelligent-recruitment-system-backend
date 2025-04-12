package com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.entity.location.Location;
import com.event.recruitment.intelligent_recruitment_system.model.enums.AuthProvider;
import com.event.recruitment.intelligent_recruitment_system.model.enums.RecruiterType;
import com.event.recruitment.intelligent_recruitment_system.model.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "recruiters")
public class Recruiters {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "recruiter_rep_name", nullable = false)
    private String recruiterRepName;

    @Column(unique = true, nullable = false)
    private String email;

    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider")
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "oauth_id")
    private String oauthId;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "recruiter_type")
    private RecruiterType recruiterType = RecruiterType.INDIVIDUAL;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_logo_url")
    private String companyLogoUrl;

    @Column(name = "company_description")
    private String companyDescription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_location_id")
    private Location companyLocation;

    @Column(name = "company_website")
    private String companyWebsite;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
package com.event.recruitment.intelligent_recruitment_system.model.entity.candidate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "candidate_availability_dates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateAvailabilityDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;
}
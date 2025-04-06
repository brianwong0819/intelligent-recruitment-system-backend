package com.event.recruitment.intelligent_recruitment_system.repository.recruiter;

import com.event.recruitment.intelligent_recruitment_system.model.entity.recruiter.RecruiterPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruiterPortfolioRepository extends JpaRepository<RecruiterPortfolio, Integer> {

    List<RecruiterPortfolio> findByRecruiterId(Integer recruiterId);

    Optional<RecruiterPortfolio> findByIdAndRecruiterId(Integer id, Integer recruiterId);
}
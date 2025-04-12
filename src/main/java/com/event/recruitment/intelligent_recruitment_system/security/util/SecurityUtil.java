// src/main/java/com/event/recruitment/intelligent_recruitment_system/security/util/SecurityUtil.java

package com.event.recruitment.intelligent_recruitment_system.security.util;

import com.event.recruitment.intelligent_recruitment_system.repository.recruiter.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private final RecruiterRepository recruiterRepository;

    @Autowired
    public SecurityUtil(RecruiterRepository recruiterRepository) {
        this.recruiterRepository = recruiterRepository;
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    /**
     * Get the ID of the currently authenticated recruiter
     * @return The recruiter ID or null if not a recruiter
     */
    public Long getCurrentRecruiterId() {
        String username = getCurrentUsername();
        return recruiterRepository.findByUsername(username)
                .map(recruiter -> recruiter.getId())
                .orElse(null);
    }
}
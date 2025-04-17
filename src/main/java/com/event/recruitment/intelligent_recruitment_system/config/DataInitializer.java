// Path: src/main/java/com/event/recruitment/intelligent_recruitment_system/config/DataInitializer.java

package com.event.recruitment.intelligent_recruitment_system.config;

import com.event.recruitment.intelligent_recruitment_system.service.auth.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdminAuthService adminAuthService;

    @Override
    public void run(ApplicationArguments args) {
        // Initialize admin user when application starts
        adminAuthService.initializeAdminUser();
    }
}
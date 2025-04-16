package com.event.recruitment.intelligent_recruitment_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.training-materials-dir}")
    private String trainingMaterialsDir;

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Expose the upload directory as a static resource
        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();

        Path trainingMaterialsUploadDir = Paths.get(trainingMaterialsDir);
        String trainingMaterialsUploadPath = trainingMaterialsUploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/api/files/**")
                .addResourceLocations("file:" + uploadAbsolutePath + "/");

        registry.addResourceHandler("/api/training/materials/**")
                .addResourceLocations("file:" + trainingMaterialsUploadPath + "/");
    }
}
package com.company.employeeportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for file upload and static resource serving.
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:/tmp/uploads/profile-pictures}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure upload directory exists
        try {
            // Use absolute path for Docker environment
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath);
            }
        } catch (Exception e) {
            // Log error but don't fail startup
            System.err.println("Failed to create upload directory: " + e.getMessage());
        }

        // Serve uploaded files
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + Paths.get(uploadDir).toAbsolutePath() + "/");
    }
}
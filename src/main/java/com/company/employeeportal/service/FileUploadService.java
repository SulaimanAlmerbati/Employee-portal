package com.company.employeeportal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file uploads, particularly profile pictures.
 */
@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);
    
    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    
    // Allowed image types
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif"
    );
    
    // Upload directory (configurable via application properties)
    @Value("${app.upload.dir:/tmp/uploads/profile-pictures}")
    private String uploadDir;

    /**
     * Upload a profile picture file.
     * 
     * @param file the uploaded file
     * @param userId the user ID for organizing files
     * @return the relative path to the uploaded file
     * @throws IOException if file upload fails
     * @throws IllegalArgumentException if file validation fails
     */
    public String uploadProfilePicture(MultipartFile file, Long userId) throws IOException {
        try {
            // Validate file
            validateFile(file);
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            logger.info("Attempting to create upload directory: {}", uploadPath);
            
            if (!Files.exists(uploadPath)) {
                try {
                    Files.createDirectories(uploadPath);
                    logger.info("Successfully created upload directory: {}", uploadPath);
                } catch (IOException e) {
                    logger.error("Failed to create upload directory: {}", uploadPath, e);
                    throw new IOException("Cannot create upload directory: " + e.getMessage(), e);
                }
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = "user_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            logger.info("Saving file to: {}", filePath);
            
            try {
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Profile picture uploaded successfully: {} for user: {}", uniqueFilename, userId);
            } catch (IOException e) {
                logger.error("Failed to save file: {}", filePath, e);
                throw new IOException("Cannot save file: " + e.getMessage(), e);
            }
            
            // Return relative path for storage in database
            return uniqueFilename; // Just return filename, not full path
            
        } catch (Exception e) {
            logger.error("Error uploading profile picture for user: {}", userId, e);
            throw e;
        }
    }
    
    /**
     * Delete a profile picture file.
     * 
     * @param filePath the path to the file to delete
     */
    public void deleteProfilePicture(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Profile picture deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Failed to delete profile picture: {}", filePath, e);
        }
    }
    
    /**
     * Validate uploaded file.
     * 
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size cannot exceed 5MB");
        }
        
        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPEG, PNG, and GIF images are allowed");
        }
        
        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
    }
    
    /**
     * Get file extension from filename.
     * 
     * @param filename the filename
     * @return the file extension including the dot (e.g., ".jpg")
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        
        return filename.substring(lastDotIndex).toLowerCase();
    }
}
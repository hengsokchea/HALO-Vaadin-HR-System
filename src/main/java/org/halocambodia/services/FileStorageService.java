package org.halocambodia.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload.dir:./uploads/policies}")
    private String uploadDir;

    @Value("${file.upload.temp-dir:./uploads/temp}")
    private String tempDir;

    private Path uploadPath;
    private Path tempPath;

    @PostConstruct
    public void init() {
        try {
            this.uploadPath = Paths.get(uploadDir);
            this.tempPath = Paths.get(tempDir);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath);
            }
            
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                log.info("Created temp directory: {}", tempPath);
            }
            
            log.info("FileStorageService initialized with upload directory: {}", uploadDir);
        } catch (IOException e) {
            log.error("Failed to initialize FileStorageService", e);
            throw new RuntimeException("Failed to initialize file storage", e);
        }
    }

    /**
     * Store file from byte array (for smaller files)
     */
    public String storeFile(byte[] fileContent, String originalFileName, UUID fileUuid) throws IOException {
        String storedFileName = generateStoredFileName(originalFileName, fileUuid);
        Path filePath = uploadPath.resolve(storedFileName);
        
        Files.write(filePath, fileContent);
        log.info("File stored successfully: {} ({} bytes)", storedFileName, fileContent.length);
        
        return storedFileName;
    }

    /**
     * Store file from InputStream with streaming (for large files - 6GB+)
     * This is the recommended method for large files
     */
    public String storeFile(InputStream inputStream, String originalFileName, UUID fileUuid, long fileSize) throws IOException {
        String storedFileName = generateStoredFileName(originalFileName, fileUuid);
        Path filePath = uploadPath.resolve(storedFileName);
        
        // Use Files.copy with StandardCopyOption for efficient streaming
        // This doesn't load the entire file into memory
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("File stored successfully: {} ({} bytes)", storedFileName, fileSize);
        
        return storedFileName;
    }

    /**
     * Load file content as byte array (for smaller files - use with caution)
     */
    public byte[] loadFile(String storedFileName) throws IOException {
        Path filePath = uploadPath.resolve(storedFileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storedFileName);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * Load file as InputStream for streaming (recommended for large files)
     */
    public InputStream loadFileAsStream(String storedFileName) throws IOException {
        Path filePath = uploadPath.resolve(storedFileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storedFileName);
        }
        return Files.newInputStream(filePath);
    }

    /**
     * Delete file from storage
     */
    public boolean deleteFile(String storedFileName) throws IOException {
        if (storedFileName == null || storedFileName.isEmpty()) {
            return false;
        }
        
        Path filePath = uploadPath.resolve(storedFileName);
        if (Files.exists(filePath)) {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted successfully: {}", storedFileName);
            }
            return deleted;
        }
        log.warn("File not found for deletion: {}", storedFileName);
        return false;
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String storedFileName) {
        if (storedFileName == null || storedFileName.isEmpty()) {
            return false;
        }
        Path filePath = uploadPath.resolve(storedFileName);
        return Files.exists(filePath);
    }

    /**
     * Get file size
     */
    public long getFileSize(String storedFileName) throws IOException {
        Path filePath = uploadPath.resolve(storedFileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storedFileName);
        }
        return Files.size(filePath);
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return null;
    }

    private String generateStoredFileName(String originalFileName, UUID fileUuid) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = getFileExtension(originalFileName);
        
        if (extension != null && !extension.isEmpty()) {
            return String.format("%s_%s.%s", fileUuid.toString(), timestamp, extension);
        } else {
            return String.format("%s_%s", fileUuid.toString(), timestamp);
        }
    }

    public Path getFilePath(String storedFileName) {
        return uploadPath.resolve(storedFileName);
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public String getTempDir() {
        return tempDir;
    }
    
    /**
     * Finalize large chunked upload
     */
    public String finalizeLargeUpload(Path tempPath, String originalFileName, UUID fileUuid) throws IOException {
        if (!Files.exists(tempPath)) {
            throw new IOException("Temp file not found: " + tempPath);
        }

        String storedFileName = generateStoredFileName(originalFileName, fileUuid);
        Path finalPath = uploadPath.resolve(storedFileName);

        Files.move(tempPath, finalPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        log.info("Finalized large upload: {} -> {} ({} bytes)", 
                 originalFileName, storedFileName, Files.size(finalPath));

        return storedFileName;
    }
}
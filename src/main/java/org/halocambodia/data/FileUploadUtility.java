package org.halocambodia.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.streams.DownloadHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Component
public class FileUploadUtility {

    @Autowired
    private Environment environment;

    @Autowired
    private AttachmentTypeRepository attachmentTypeRepository;

    public AttachmentType getAttachmentTypeById(Long id) {
        return attachmentTypeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("AttachmentType ID " + id + " not found"));
    }

    public Path initializeUploadDirectory(String appName, String subDirectory) {
        boolean isProduction = Boolean.parseBoolean(environment.getProperty("vaadin.productionMode", "false"));
        System.out.println("Running in " + (isProduction ? "PRODUCTION" : "DEVELOPMENT") + " mode");

        Path uploadDir;
        if (isProduction) {
            String basePath = System.getProperty("catalina.base", System.getProperty("user.dir"));
            uploadDir = Paths.get(basePath, "webapps", appName, "uploads", subDirectory);
        } else {
            String projectPath = System.getProperty("user.dir");
            uploadDir = Paths.get(projectPath, "uploads", subDirectory);
        }

        try {
            Files.createDirectories(uploadDir);
            return uploadDir;
        } catch (IOException e) {
            throw new RuntimeException("Unable to initialize upload directory for " + subDirectory, e);
        }
    }

    public String handleFileUpload(Path uploadDir,
                                   String subDirectory,
                                   InputStream inputStream,
                                   String originalFileName,
                                   String currentFilePath) throws IOException {
        Path fullUploadDir = uploadDir.resolve(subDirectory);
        Files.createDirectories(fullUploadDir);

        if (currentFilePath != null && !currentFilePath.isEmpty()) {
            try {
                Path oldPath = fullUploadDir.resolve(currentFilePath);
                Files.deleteIfExists(oldPath);
            } catch (IOException e) {
                System.err.println("Warning: Could not delete old file: " + e.getMessage());
            }
        }

        String uniqueFileName = UUID.randomUUID() + "-" + originalFileName;
        Path uploadPath = fullUploadDir.resolve(uniqueFileName);
        Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Uploaded file to: " + uploadPath);

        return uniqueFileName;
    }

    public Boolean handleFileUploadFromDB(Path uploadDir, String subDirectory,
                                          InputStream inputStream, String originalFileName) {
        try {
            Path fullUploadDir = uploadDir.resolve(subDirectory);
            Files.createDirectories(fullUploadDir);

            Path uploadPath = fullUploadDir.resolve(originalFileName);
            Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("Uploaded file to: " + uploadPath);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to upload file: " + originalFileName);
            e.printStackTrace();
            return false;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                System.err.println("Failed to close input stream for: " + originalFileName);
            }
        }
    }

    /**
     * Create an Anchor for downloading a file (replacement for StreamResource usage).
     */
    public Anchor createFileDownloadLink(Path baseDir, String fileName, String linkText) {
        DownloadHandler handler = event -> {
            try {
                Path filePath = baseDir.resolve(fileName);
                event.setFileName(fileName);
                try (InputStream is = Files.newInputStream(filePath);
                     OutputStream os = event.getOutputStream()) {
                    is.transferTo(os);
                }
            } catch (IOException e) {
                System.err.println("Failed to stream file: " + fileName);
            }
        };
        return new Anchor(handler, linkText);
    }

    /**
     * Create an Image component backed by DownloadHandler (replaces StreamResource).
     */
    public Image createImageComponent(Path baseDir, String fileName, String altText) {
        DownloadHandler handler = event -> {
            try {
                Path filePath = baseDir.resolve(fileName);
                event.setFileName(fileName);
                try (InputStream is = Files.newInputStream(filePath);
                     OutputStream os = event.getOutputStream()) {
                    is.transferTo(os);
                }
            } catch (IOException e) {
                try (InputStream fallback = getClass().getResourceAsStream("/assets/userProfile/profile.jpg")) {
                    if (fallback != null) {
                        fallback.transferTo(event.getOutputStream());
                    }
                } catch (IOException ex) {
                    System.err.println("Failed to stream fallback image");
                }
            }
        };
        return new Image(handler, altText);
    }

    /**
     * Create a default profile image if nothing is set.
     */
    public Image getDefaultImage() {
        DownloadHandler handler = event -> {
            try (InputStream fallback = getClass().getResourceAsStream("/assets/userProfile/profile.jpg")) {
                if (fallback != null) {
                    fallback.transferTo(event.getOutputStream());
                }
            } catch (IOException e) {
                System.err.println("Failed to stream default profile image");
            }
        };
        return new Image(handler, "Default Profile");
    }
    
    public String getAvatarImageUrl(String subDirectory, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "/images/default-profile.png"; // static fallback
        }

        // Construct full file path
        Path baseDir = initializeUploadDirectory("hr", subDirectory);
        Path filePath = baseDir.resolve(fileName);

        if (Files.exists(filePath)) {
            // ✅ Option A: return as a public URL (recommended, if uploads are served by Tomcat/Spring Boot)
            //return "/uploads/" + subDirectory + "/" + fileName;

            // ✅ Option B: If you don’t expose /uploads publicly, switch to Base64 embedding:
            
            try {
                byte[] bytes = Files.readAllBytes(filePath);
                String base64 = Base64.getEncoder().encodeToString(bytes);
                String mimeType = fileName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                return "data:" + mimeType + ";base64," + base64;
            } catch (IOException e) {
                return "/images/default-profile.png";
            }
            
        } else {
            return "/images/default-profile.png";
        }
    }
}

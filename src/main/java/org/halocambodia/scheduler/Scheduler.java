package org.halocambodia.scheduler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.halocambodia.data.*;
import org.halocambodia.data.AttachmentRepository;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


@Component
@RequiredArgsConstructor
public class Scheduler {

    private final SchedulerService importerService;
  
    private final AttachmentRepository attachmentRepository;
    
    private  Path uploadDir;
    private static final String UserProfileSubDirectory="attachments";
    private final  FileUploadUtility fileUploadUtility;

    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void schedulerData() {
      
    	// Schedule for Data Feed
        try {
        	System.out.println("Starting run scheduled at: " + LocalDateTime.now());
           importerService.runImport();           
            System.out.println("Completed run scheduled at: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Failed run scheduled  at " + LocalDateTime.now() + ": " + e.getMessage());
            e.printStackTrace();         
        }
       
        // Schedule for Attachment
      
        try {
            System.out.println("Starting run Attachment at: " + LocalDateTime.now());
            uploadDir=fileUploadUtility.initializeUploadDirectory("hr", "");

            List<Attachment> attachments = attachmentRepository.findByFileDataIsNotNull();

            for (Attachment attachment : attachments) {
                System.out.println("Attachment ID: " + attachment.getId());
                System.out.println("Attachment Name: " + attachment.getFileName());

                byte[] fileBytes = attachment.getFileData();
                if (fileBytes != null && attachment.getFileName() != null && !attachment.getFileName().contains("/")) {
                    Boolean uploadSuccess = fileUploadUtility.handleFileUploadFromDB(uploadDir,UserProfileSubDirectory, new ByteArrayInputStream(fileBytes), attachment.getFileName());
                    if(uploadSuccess) {
                    	attachment.setFileData(null);
                    	attachmentRepository.save(attachment);
                    }
                }
            }
           
            System.out.println("Completed run Attachment at: " + LocalDateTime.now());
        } catch (Exception e) {
            System.err.println("Failed run Attachment at " + LocalDateTime.now() + ": " + e.getMessage());
            e.printStackTrace();
        }   
      
       
      
        
        
    }
   
}
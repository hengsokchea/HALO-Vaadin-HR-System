package org.halocambodia.views.employee;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.halocambodia.data.Attachment;
import org.halocambodia.data.AttachmentType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.MigrateAttachment;
import org.halocambodia.data.MigrateAttachmentRepository;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.views.MainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Route(value = "migrate", layout = MainLayout.class)
@PageTitle("Migrate Attachments")
@PermitAll
@Uses(Icon.class)
public class MigrateAttachmentView extends VerticalLayout {
    
    private TextArea logArea;
    private ProgressBar progressBar;
    private Button migrateButton;
    
    private static final String PHOTOS_PATH = "Y:/xampp/htdocs/hr/attachment";
    
    
    @Autowired
    private MigrationService migrationService;
    
    public MigrateAttachmentView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        
        com.vaadin.flow.component.html.H2 title = new com.vaadin.flow.component.html.H2("Attachment Migration Tool");
        com.vaadin.flow.component.html.Paragraph description = 
            new com.vaadin.flow.component.html.Paragraph("Migrating attachments from: " + PHOTOS_PATH);
        
        logArea = new TextArea("Migration Log");
        logArea.setWidth("90%");
        logArea.setHeight("400px");
        logArea.setReadOnly(true);
        
        progressBar = new ProgressBar();
        progressBar.setWidth("90%");
        progressBar.setVisible(false);
        
        migrateButton = new Button("Start Migration", e -> migrateProcess());
        migrateButton.setIcon(VaadinIcon.CLOUD_UPLOAD.create());
        
        add(title, description, migrateButton, progressBar, logArea);
    }
    
    private void migrateProcess() {
        migrateButton.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        logArea.clear();
        
        new Thread(() -> {
            try {
                List<MigrateAttachment> pendingMigrations = migrationService.getPendingMigrations();
                int totalRecords = pendingMigrations.size();
                
                getUI().ifPresent(ui -> ui.access(() -> {
                    logArea.setValue("Found " + totalRecords + " records to migrate.\n");
                }));
                
                if (totalRecords == 0) {
                    getUI().ifPresent(ui -> ui.access(() -> {
                        logArea.setValue("No pending migrations found.");
                        Notification.show("No records to migrate", 3000, Notification.Position.MIDDLE);
                    }));
                    return;
                }
                
                // Use AtomicInteger for mutable variables in lambdas
                AtomicInteger processed = new AtomicInteger(0);
                AtomicInteger success = new AtomicInteger(0);
                AtomicInteger failed = new AtomicInteger(0);
                
                for (MigrateAttachment migrateAttachment : pendingMigrations) {
                    final MigrateAttachment currentRecord = migrateAttachment;
                    final int currentProgress = processed.incrementAndGet();
                    
                    try {
                        boolean result = migrationService.migrateAttachment(currentRecord);
                        
                        getUI().ifPresent(ui -> ui.access(() -> {
                            if (result) {
                                success.incrementAndGet();
                                logArea.setValue(logArea.getValue() + 
                                    String.format("✓ Migrated: emp_id=%d, file=%s%n", 
                                        currentRecord.getEmpId(), 
                                        currentRecord.getFileName()));
                            } else {
                                failed.incrementAndGet();
                                logArea.setValue(logArea.getValue() + 
                                    String.format("✗ Failed: emp_id=%d, file=%s - File not found%n", 
                                        currentRecord.getEmpId(), 
                                        currentRecord.getFileName()));
                            }
                            progressBar.setValue((double) currentProgress / totalRecords);
                        }));
                        
                    } catch (Exception ex) {
                        failed.incrementAndGet();
                        getUI().ifPresent(ui -> ui.access(() -> {
                            logArea.setValue(logArea.getValue() + 
                                String.format("✗ Error: emp_id=%d, file=%s - %s%n", 
                                    currentRecord.getEmpId(), 
                                    currentRecord.getFileName(),
                                    ex.getMessage()));
                            progressBar.setValue((double) currentProgress / totalRecords);
                        }));
                    }
                    
                    Thread.sleep(50);
                }
                
                final int finalSuccess = success.get();
                final int finalFailed = failed.get();
                getUI().ifPresent(ui -> ui.access(() -> {
                    logArea.setValue(logArea.getValue() + 
                        String.format("%n✅ Migration completed! Success: %d, Failed: %d", 
                            finalSuccess, finalFailed));
                    Notification.show(
                        String.format("Migration completed! Success: %d, Failed: %d", 
                            finalSuccess, finalFailed), 
                        5000, Notification.Position.MIDDLE);
                }));
                
            } catch (Exception e) {
                getUI().ifPresent(ui -> ui.access(() -> {
                    logArea.setValue("Migration failed: " + e.getMessage());
                    Notification.show("Migration failed!", 3000, Notification.Position.MIDDLE);
                }));
            } finally {
                getUI().ifPresent(ui -> ui.access(() -> {
                    migrateButton.setEnabled(true);
                    progressBar.setVisible(false);
                }));
            }
        }).start();
    }
}

@Service
@Transactional
class MigrationService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private MigrateAttachmentRepository migrateAttachmentRepository;
    
    @Autowired
    private AttachmentTypeRepository attachmentTypeRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    private static final String PHOTOS_PATH = "Y:/xampp/htdocs/hr/attachment";
    
    public List<MigrateAttachment> getPendingMigrations() {
        return migrateAttachmentRepository.findPendingMigrations();
    }
    
    public boolean migrateAttachment(MigrateAttachment migrateAttachment) {
        try {
            String fileName = migrateAttachment.getFileName();
            String filePath = PHOTOS_PATH + "/" + fileName;
            File file = new File(filePath);
            

            
            if (!file.exists()) {
                updateMigrationStatus(migrateAttachment, "FILE_NOT_FOUND");
                return false;
            }
            
            UUID survey123Id = UUID.randomUUID();
            String uniqueFileName = survey123Id.toString() + "_" + fileName;
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String contentType = determineContentType(fileName);
            
            AttachmentType attachmentType = attachmentTypeRepository.findById(Long.valueOf(migrateAttachment.getAttachmentTypeId()))
                .orElseThrow(() -> new RuntimeException("Attachment type not found: " + migrateAttachment.getAttachmentTypeId()));
            
            Employee employee = employeeRepository.findById(Long.valueOf( migrateAttachment.getEmpId()))
                .orElseThrow(() -> new RuntimeException("Employee not found: " + migrateAttachment.getEmpId()));
            
            Attachment attachment = new Attachment();
            attachment.setAttachmentType(attachmentType);
            attachment.setFileName(uniqueFileName);
            attachment.setContentType(contentType);
            attachment.setDataSize((long) fileContent.length);
            attachment.setFileData(fileContent);
            attachment.setRemark( migrateAttachment.getNote()!=null? migrateAttachment.getNote(): "Migrated from migrate_attachment ID: " + migrateAttachment.getId());
            attachment.setAttachmentEntityTable(employee);
            attachment.setSurvey123Id(survey123Id);
            //attachment.setCreatedBy(1L);
            //attachment.setUpdatedBy(1L);
            //attachment.setCreatedAt(OffsetDateTime.now());
            //attachment.setUpdatedAt(OffsetDateTime.now());
            
            entityManager.persist(attachment);
            updateMigrationStatus(migrateAttachment, "MIGRATED");
            
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
            updateMigrationStatus(migrateAttachment, "ERROR: " + e.getMessage());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            updateMigrationStatus(migrateAttachment, "ERROR: " + e.getMessage());
            return false;
        }
    }
    
    private void updateMigrationStatus(MigrateAttachment migrateAttachment, String status) {
        migrateAttachment.setStatus(status);
        entityManager.merge(migrateAttachment);
    }
    
    private String determineContentType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "application/octet-stream";
        }
        
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt":
                return "text/plain";
            case "csv":
                return "text/csv";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "zip":
                return "application/zip";
            default:
                return "application/octet-stream";
        }
    }
}
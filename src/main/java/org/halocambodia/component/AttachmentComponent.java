package org.halocambodia.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileBuffer;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;

import org.halocambodia.data.AbstractEntity;
import org.halocambodia.data.Attachment;
import org.halocambodia.data.AttachmentRepository;
import org.halocambodia.data.AttachmentType;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.User;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.utility.FileSizeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.vaadin.flow.component.html.IFrame;

/**
 * Reusable attachment component for any entity.
 * Provides upload, preview, download, and management of attachments.
 * Works with Hibernate @Any polymorphic association.
 * Uses disk-based storage to avoid OutOfMemoryError with large files.
 * 
 * SOLUTION 1 IMPLEMENTATION: Supports both new and existing entities.
 * - For new entities: Attachments are stored in pending list, saved after entity is persisted
 * - For existing entities: Attachments are saved immediately to database
 * 
 * Also supports view-only mode where Add button is hidden and only Download action is available.
 */
public class AttachmentComponent extends VerticalLayout {

    private final Grid<Attachment> grid = new Grid<>(Attachment.class, false);
    private final List<Attachment> attachments = new ArrayList<>();
    private final String entityDiscriminator;
    private AbstractEntity entity;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentTypeRepository attachmentTypeRepository;
    private final FileUploadUtility fileUploadUtility;
    private final Optional<User> currentUser;
    private final Path uploadDir;
    private final String attachmentsSubDirectory = "attachments";
    private final Runnable onAttachmentChange;
    
    // SOLUTION 1: Fields for handling new entity attachments
    private boolean isNewEntity = false;
    private final List<Attachment> pendingAttachments = new ArrayList<>();
    
    // View-only mode flag
    private boolean viewOnlyMode = false;
    
    // UI Components that need to be refreshed
    private HorizontalLayout headerLayout;
    private Button btnAdd;

    /**
     * Constructor with view-only mode option
     */
    public AttachmentComponent(
            String entityDiscriminator,
            AbstractEntity entity,
            AttachmentRepository attachmentRepository,
            AttachmentTypeRepository attachmentTypeRepository,
            FileUploadUtility fileUploadUtility,
            Optional<User> currentUser,
            Path baseUploadDir,
            Runnable onAttachmentChange,
            boolean viewOnlyMode) {
        
        this.entityDiscriminator = entityDiscriminator;
        this.entity = entity;
        this.attachmentRepository = attachmentRepository;
        this.attachmentTypeRepository = attachmentTypeRepository;
        this.fileUploadUtility = fileUploadUtility;
        this.currentUser = currentUser;
        //this.uploadDir = fileUploadUtility.initializeUploadDirectory(baseUploadDir.toString(), attachmentsSubDirectory);
        this.uploadDir = baseUploadDir.resolve(attachmentsSubDirectory);
        this.onAttachmentChange = onAttachmentChange != null ? onAttachmentChange : () -> {};
        this.viewOnlyMode = viewOnlyMode;

        initComponent();
        loadAttachments();
    }

    /**
     * Simplified constructor without callback (defaults to edit mode)
     */
    public AttachmentComponent(
            String entityDiscriminator,
            AbstractEntity entity,
            AttachmentRepository attachmentRepository,
            AttachmentTypeRepository attachmentTypeRepository,
            FileUploadUtility fileUploadUtility,
            Optional<User> currentUser,
            Path baseUploadDir) {
        this(entityDiscriminator, entity, attachmentRepository, attachmentTypeRepository, 
             fileUploadUtility, currentUser, baseUploadDir, null, false);
    }
    
    /**
     * Constructor with view-only mode but without callback
     */
    public AttachmentComponent(
            String entityDiscriminator,
            AbstractEntity entity,
            AttachmentRepository attachmentRepository,
            AttachmentTypeRepository attachmentTypeRepository,
            FileUploadUtility fileUploadUtility,
            Optional<User> currentUser,
            Path baseUploadDir,
            boolean viewOnlyMode) {
        this(entityDiscriminator, entity, attachmentRepository, attachmentTypeRepository, 
             fileUploadUtility, currentUser, baseUploadDir, null, viewOnlyMode);
    }

    /**
     * SOLUTION 1: Set whether this component is in "new entity" mode
     * @param isNew true if the parent entity is new (not yet persisted), false otherwise
     */
    public void setNewEntityMode(boolean isNew) {
        this.isNewEntity = isNew;
        if (isNew) {
            pendingAttachments.clear();
        }
    }
    
    /**
     * SOLUTION 1: Get pending attachments that need to be saved after entity is persisted
     * @return List of pending attachments
     */
    public List<Attachment> getPendingAttachments() {
        return new ArrayList<>(pendingAttachments);
    }
    
    /**
     * SOLUTION 1: Clear pending attachments after they've been saved
     */
    public void clearPendingAttachments() {
        pendingAttachments.clear();
    }
    
    /**
     * Set view-only mode
     * @param viewOnly true for view-only mode, false for edit mode
     */
    public void setViewOnlyMode(boolean viewOnly) {
        if (this.viewOnlyMode != viewOnly) {
            this.viewOnlyMode = viewOnly;
            updateUIMode();
        }
    }
    
    /**
     * Update UI based on current mode without full recreation
     */
    private void updateUIMode() {
        // Update Add button visibility
        if (btnAdd != null) {
            btnAdd.setVisible(!viewOnlyMode);
        }
        
        // Refresh grid columns for actions
        refreshGridColumns();
    }
    
    /**
     * Refresh grid columns based on current mode
     */
    private void refreshGridColumns() {
        // Remove all existing columns
        grid.removeAllColumns();
        
        // Re-add all columns with proper action buttons
        grid.addColumn(a -> a.getAttachmentType() != null ? 
                a.getAttachmentType().getAttachmentTypeName() : "")
            .setHeader("Attachment Type | ប្រភេទឯកសារ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.addColumn(new com.vaadin.flow.data.renderer.ComponentRenderer<>(this::buildFileNameCell))
            .setHeader("File Name | ឈ្មោះឯកសារ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.addColumn(a -> a.getDataSize() != null ? 
                FileSizeFormatter.formatBytes(a.getDataSize()) : "")
            .setHeader("Size | ទំហំ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);



        grid.addColumn(a -> a.getRemark() != null ? a.getRemark() : "")
            .setHeader("Remark | កំណត់សម្គាល់")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        // Actions column based on mode
        if (viewOnlyMode) {
            grid.addComponentColumn(this::buildViewOnlyActionButtons)
                .setHeader("Actions | សកម្មភាព")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
        } else {
            grid.addComponentColumn(this::buildEditModeActionButtons)
                .setHeader("Actions | សកម្មភាព")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
        }
        grid.addColumn(a -> a.getSurvey123Id() != null ? 
                a.getSurvey123Id().toString() : "")
            .setHeader("Survey123 ID")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);
        // Audit columns
        grid.addColumn(a -> a.getUserCreated() != null ? a.getUserCreated().getName() : "")
            .setHeader("Created By | បង្កើតដោយ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.addColumn(a -> a.getCreatedAt() != null ? 
                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(a.getCreatedAt()) : "")
            .setHeader("Created At | ថ្ងៃបង្កើត")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.addColumn(a -> a.getUserUpdated() != null ? a.getUserUpdated().getName() : "")
            .setHeader("Updated By | កែប្រែដោយ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.addColumn(a -> a.getUpdatedAt() != null ? 
                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(a.getUpdatedAt()) : "")
            .setHeader("Updated At | ថ្ងៃកែប្រែ")
            .setSortable(true)
            .setResizable(true)
            .setAutoWidth(true);

        grid.setItems(attachments);
    }

    private void initComponent() {
        setPadding(false);
        setSpacing(true);
        setMargin(false);
        setWidthFull();

        // Header with title
        headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(true);
        headerLayout.setSpacing(true);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        
        // Title
        H4 title = new H4("Attachments | ឯកសារភ្ជាប់");
        title.getStyle()
            .set("margin", "0")
            .set("font-size", "var(--lumo-font-size-l)")
            .set("font-weight", "500");
        
        headerLayout.add(title);
        
        // Add button - visibility controlled by viewOnlyMode
        btnAdd = new Button("Add Document | បន្ថែមឯកសារ", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        btnAdd.addClickListener(e -> openAttachmentDialog(null));
        btnAdd.setVisible(!viewOnlyMode);
        headerLayout.add(btnAdd);
        
        headerLayout.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
            .set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
            .set("border-radius", "var(--lumo-border-radius-m) var(--lumo-border-radius-m) 0 0");

        configureGrid();
        add(headerLayout, grid);
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(
            GridVariant.LUMO_COLUMN_BORDERS,
            GridVariant.LUMO_ROW_STRIPES,
            GridVariant.LUMO_WRAP_CELL_CONTENT
        );

        refreshGridColumns();
    }

    private Component buildFileNameCell(Attachment attachment) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setMargin(false);

        Icon icon = getFileIcon(attachment.getContentType());
        icon.setSize("20px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        String displayName = attachment.getFileName();
        if (displayName != null && displayName.contains("_")) {
            String[] parts = displayName.split("_", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                displayName = parts[1];
            }
        }

        Span fileName = new Span(displayName != null ? displayName : "Unnamed");
        fileName.getStyle()
            .set("cursor", "pointer")
            .set("color", "var(--lumo-primary-text-color)")
            .set("text-decoration", "underline");
        
        fileName.addClickListener(e -> previewAttachment(attachment));

        layout.add(icon, fileName);
        return layout;
    }

    /**
     * Build action buttons for edit mode - Download, Edit, Delete
     */
    private HorizontalLayout buildEditModeActionButtons(Attachment attachment) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setPadding(false);
        actions.setSpacing(false);
        actions.setMargin(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // Download button
        Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        downloadBtn.setTooltipText("Download | ទាញយក");
        downloadBtn.setEnabled(Files.exists(uploadDir.resolve(attachment.getFileName())));
        downloadBtn.addClickListener(e -> downloadAttachment(attachment));

        // Edit button
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        editBtn.setTooltipText("Edit | កែប្រែ");
        editBtn.addClickListener(e -> openEditDialog(attachment));

        // Delete button
        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        deleteBtn.setTooltipText("Delete | លុប");
        deleteBtn.addClickListener(e -> confirmDelete(attachment));

        actions.add(downloadBtn, editBtn, deleteBtn);
        return actions;
    }
    
    /**
     * Build action buttons for view-only mode - Only Download
     */
    private HorizontalLayout buildViewOnlyActionButtons(Attachment attachment) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setMargin(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        actions.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // Download button only
        Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        downloadBtn.setTooltipText("Download | ទាញយក");
        downloadBtn.setEnabled(Files.exists(uploadDir.resolve(attachment.getFileName())));
        downloadBtn.addClickListener(e -> downloadAttachment(attachment));

        actions.add(downloadBtn);
        return actions;
    }

    private Icon getFileIcon(String contentType) {
        if (contentType == null) return VaadinIcon.FILE.create();
        
        if (contentType.startsWith("image/")) {
            return VaadinIcon.PICTURE.create();
        } else if (contentType.startsWith("video/")) {
            return VaadinIcon.FILM.create();
        } else if (contentType.startsWith("audio/")) {
            return VaadinIcon.MUSIC.create();
        } else if (contentType.equals("application/pdf")) {
            return VaadinIcon.FILE_PRESENTATION.create();
        } else if (contentType.contains("spreadsheet") || 
                   contentType.contains("excel") || 
                   contentType.contains("csv")) {
            return VaadinIcon.FILE_TABLE.create();
        } else if (contentType.contains("document") || 
                   contentType.contains("word") || 
                   contentType.contains("text")) {
            return VaadinIcon.FILE_TEXT.create();
        }
        return VaadinIcon.FILE.create();
    }

    /**
     * SOLUTION 1: Load attachments based on entity state
     */
    private void loadAttachments() {
        attachments.clear();
        
        if (entity != null) {
            if (entity.getId() != null) {
                // Existing entity - load from database
                try {
                    List<Attachment> dbAttachments = attachmentRepository.findByEntity(entity);
                    attachments.addAll(dbAttachments);
                } catch (Exception e) {
                    Notification.show("Error loading attachments: " + e.getMessage(),
                        3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    e.printStackTrace();
                }
            } else {
                // New entity - load from pending attachments
                attachments.addAll(pendingAttachments);
            }
        }
        
        grid.setItems(attachments);
        grid.getDataProvider().refreshAll();
    }

    /**
     * Open dialog for adding new attachments - Only called in edit mode
     */
    private void openAttachmentDialog(Attachment editing) {
        // Prevent opening dialog in view-only mode
        if (viewOnlyMode) {
            return;
        }
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Attachment | បន្ថែមឯកសារ");
        dialog.setWidth("900px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);

        // Attachment Type ComboBox
        ComboBox<AttachmentType> attachmentTypeCombo = new ComboBox<>("Attachment Type | ប្រភេទឯកសារ");
        attachmentTypeCombo.setItems(attachmentTypeRepository.findActiveAttachmentTypes());
        attachmentTypeCombo.setItemLabelGenerator(AttachmentType::getAttachmentTypeName);
        attachmentTypeCombo.setRequiredIndicatorVisible(true);
        attachmentTypeCombo.setWidthFull();
        attachmentTypeCombo.setClearButtonVisible(true);

        // Remark TextArea
        TextArea remarkField = new TextArea("Remark | កំណត់សម្គាល់");
        remarkField.setWidthFull();
        remarkField.setPlaceholder("Enter remarks for this attachment... | បញ្ចូលកំណត់សម្គាល់សម្រាប់ឯកសារនេះ...");
        remarkField.setClearButtonVisible(true);
        remarkField.setValue("");

        // Multi-file Upload - DISK BASED
        MultiFileBuffer buffer = new MultiFileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes();
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
        upload.setMaxFiles(100);
        upload.setWidthFull();

        List<Attachment> newAttachments = new ArrayList<>();

        // Preview Container
        VerticalLayout previewLayout = new VerticalLayout();
        previewLayout.setPadding(false);
        previewLayout.setSpacing(false);
        previewLayout.setWidthFull();

        // Upload Listeners
        upload.addSucceededListener(event -> {
            try {
                InputStream inputStream = buffer.getInputStream(event.getFileName());
                UUID survey123Id = UUID.randomUUID();
                String uniqueFileName = survey123Id.toString() + "_" + event.getFileName();
                Path targetPath = uploadDir.resolve(uniqueFileName);
                
                Files.createDirectories(uploadDir);
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                long fileSize = Files.size(targetPath);

                Attachment newAttachment = new Attachment();
                newAttachment.setFileName(uniqueFileName);
                newAttachment.setContentType(event.getMIMEType());
                newAttachment.setFileData(null);
                newAttachment.setDataSize(fileSize);
                newAttachment.setSurvey123Id(survey123Id);
                newAttachment.setRemark(remarkField.getValue() != null ? remarkField.getValue() : "");
                
                if (attachmentTypeCombo.getValue() != null) {
                    newAttachment.setAttachmentType(attachmentTypeCombo.getValue());
                }
                
                newAttachments.add(newAttachment);

                if (event.getMIMEType() != null && event.getMIMEType().startsWith("image/")) {
                    byte[] previewBytes = Files.readAllBytes(targetPath);
                    Component preview = buildFilePreview(
                        uniqueFileName, 
                        event.getMIMEType(), 
                        previewBytes, 
                        newAttachments, 
                        newAttachment,
                        previewLayout
                    );
                    previewLayout.add(preview);
                } else {
                    Component preview = buildFileInfoPreview(
                        uniqueFileName, 
                        event.getMIMEType(), 
                        fileSize, 
                        newAttachments, 
                        newAttachment,
                        previewLayout
                    );
                    previewLayout.add(preview);
                }

                Notification.show(event.getFileName() + " uploaded.",
                    1500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (IOException ex) {
                Notification.show("Error saving file: " + ex.getMessage(),
                    2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });

        upload.addFileRejectedListener(e ->
            Notification.show(e.getErrorMessage(), 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));

        upload.addFailedListener(e ->
            Notification.show("Failed to upload: " + e.getFileName(),
                2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(attachmentTypeCombo, 1);
        form.add(upload, 1);
        form.add(remarkField, 1);
        form.add(previewLayout, 1);

        // Buttons
        Button btnSave = new Button("Save | រក្សាទុក", VaadinIcon.CHECK.create(), e -> {
            try {
                if (attachmentTypeCombo.getValue() == null) {
                    Notification.show("Please select Attachment Type | សូមជ្រើសរើសប្រភេទឯកសារ",
                        2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                if (newAttachments.isEmpty()) {
                    Notification.show("Please upload at least one file | សូមបង្ហោះឯកសារយ៉ាងហោចណាស់មួយ",
                        2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // SOLUTION 1: Handle based on entity state
                if (isNewEntity || entity == null || entity.getId() == null) {
                    // NEW ENTITY: Add to pending list
                    for (Attachment a : newAttachments) {
                        a.setAttachmentType(attachmentTypeCombo.getValue());
                        a.setRemark(remarkField.getValue() != null ? remarkField.getValue() : "");
                        
                        currentUser.ifPresent(u -> {
                            a.setUserCreated(u);
                            a.setUserUpdated(u);
                        });
                        
                        pendingAttachments.add(a);
                        attachments.add(a);
                    }
                    
                    Notification.show(newAttachments.size() + " file(s) added (will be saved with employee)",
                        2200, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    // EXISTING ENTITY: Save immediately
                    for (Attachment a : newAttachments) {
                        a.setAttachmentType(attachmentTypeCombo.getValue());
                        a.setAttachmentEntityTable(entity);
                        a.setRemark(remarkField.getValue() != null ? remarkField.getValue() : "");
                        
                        currentUser.ifPresent(u -> {
                            a.setUserCreated(u);
                            a.setUserUpdated(u);
                        });
                        
                        attachmentRepository.save(a);
                        attachments.add(a);
                    }
                    
                    Notification.show(newAttachments.size() + " file(s) saved | បានរក្សាទុក",
                        2200, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }

                dialog.close();
                grid.setItems(attachments);
                onAttachmentChange.run();

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(),
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnCancel = new Button("Cancel | បោះបង់", VaadinIcon.CLOSE.create(), e -> dialog.close());
        btnCancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footer = new HorizontalLayout(btnCancel, btnSave);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(form, footer);
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        dialogLayout.setMargin(false);
        dialogLayout.setWidthFull();

        dialog.add(dialogLayout);
        dialog.open();
    }

    /**
     * Open dialog for editing attachment - Only called in edit mode
     */
    private void openEditDialog(Attachment attachment) {
        // Prevent opening dialog in view-only mode
        if (viewOnlyMode) {
            return;
        }
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Attachment | កែប្រែឯកសារ");
        dialog.setWidth("900px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);

        ComboBox<AttachmentType> attachmentTypeCombo = new ComboBox<>("Attachment Type | ប្រភេទឯកសារ");
        attachmentTypeCombo.setItems(attachmentTypeRepository.findActiveAttachmentTypes());
        attachmentTypeCombo.setItemLabelGenerator(AttachmentType::getAttachmentTypeName);
        attachmentTypeCombo.setRequiredIndicatorVisible(true);
        attachmentTypeCombo.setWidthFull();
        attachmentTypeCombo.setClearButtonVisible(true);
        attachmentTypeCombo.setValue(attachment.getAttachmentType());

        TextArea remarkField = new TextArea("Remark | កំណត់សម្គាល់");
        remarkField.setWidthFull();
        remarkField.setPlaceholder("Enter remarks for this attachment... | បញ្ចូលកំណត់សម្គាល់សម្រាប់ឯកសារនេះ...");
        remarkField.setClearButtonVisible(true);
        remarkField.setValue(attachment.getRemark() != null ? attachment.getRemark() : "");

        String displayName = attachment.getFileName();
        if (displayName != null && displayName.contains("_")) {
            String[] parts = displayName.split("_", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                displayName = parts[1];
            }
        }

        Span currentFileInfo = new Span("Current file: " + (displayName != null ? displayName : attachment.getFileName()));
        currentFileInfo.getStyle()
            .set("font-size", "0.875rem")
            .set("padding", "var(--lumo-space-s)")
            .set("background-color", "var(--lumo-contrast-10pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("width", "100%");

        Span replaceFileTitle = new Span("Replace file (optional) | ជំនួសឯកសារ (មិនចាំបាច់)");
        replaceFileTitle.getStyle()
            .set("font-weight", "600")
            .set("margin-top", "var(--lumo-space-m)")
            .set("margin-bottom", "var(--lumo-space-xs)");

        MultiFileBuffer buffer = new MultiFileBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes();
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
        upload.setMaxFiles(1);
        upload.setWidthFull();

        final String[] newFileName = {null};
        final String[] newContentType = {null};
        final Long[] newFileSize = {null};

        VerticalLayout previewLayout = new VerticalLayout();
        previewLayout.setPadding(false);
        previewLayout.setSpacing(false);
        previewLayout.setWidthFull();

        upload.addSucceededListener(event -> {
            try {
                Path oldFilePath = uploadDir.resolve(attachment.getFileName());
                Files.deleteIfExists(oldFilePath);
                
                InputStream inputStream = buffer.getInputStream(event.getFileName());
                
                UUID survey123Id = attachment.getSurvey123Id();
                if (survey123Id == null) {
                    survey123Id = UUID.randomUUID();
                }
                
                String uniqueFileName = survey123Id.toString() + "_" + event.getFileName();
                Path targetPath = uploadDir.resolve(uniqueFileName);
                
                Files.createDirectories(uploadDir);
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                long fileSize = Files.size(targetPath);
                
                newFileName[0] = uniqueFileName;
                newContentType[0] = event.getMIMEType();
                newFileSize[0] = fileSize;

                previewLayout.removeAll();

                if (event.getMIMEType() != null && event.getMIMEType().startsWith("image/")) {
                    byte[] previewBytes = Files.readAllBytes(targetPath);
                    Component preview = buildFilePreview(
                        uniqueFileName, 
                        event.getMIMEType(), 
                        previewBytes, 
                        null, 
                        null,
                        previewLayout
                    );
                    previewLayout.add(preview);
                } else {
                    Component preview = buildFileInfoPreview(
                        uniqueFileName, 
                        event.getMIMEType(), 
                        fileSize, 
                        null, 
                        null,
                        previewLayout
                    );
                    previewLayout.add(preview);
                }

                Notification.show("File replaced: " + event.getFileName(),
                    1500, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (IOException ex) {
                Notification.show("Error replacing file: " + ex.getMessage(),
                    2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });

        upload.addFileRejectedListener(e ->
            Notification.show(e.getErrorMessage(), 2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));

        upload.addFailedListener(e ->
            Notification.show("Failed to upload: " + e.getFileName(),
                2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(currentFileInfo, 1);
        form.add(attachmentTypeCombo, 1);
        form.add(remarkField, 1);
        form.add(replaceFileTitle, 1);
        form.add(upload, 1);
        form.add(previewLayout, 1);

        Button btnSave = new Button("Update | ធ្វើបច្ចុប្បន្នភាព", VaadinIcon.CHECK.create(), e -> {
            try {
                if (attachmentTypeCombo.getValue() == null) {
                    Notification.show("Please select Attachment Type | សូមជ្រើសរើសប្រភេទឯកសារ",
                        2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // Update attachment fields
                attachment.setAttachmentType(attachmentTypeCombo.getValue());
                attachment.setRemark(remarkField.getValue());
                
                if (newFileName[0] != null) {
                    attachment.setFileName(newFileName[0]);
                    attachment.setContentType(newContentType[0]);
                    attachment.setDataSize(newFileSize[0]);
                    attachment.setFileData(null);
                }
                
                currentUser.ifPresent(attachment::setUserUpdated);
                
                // SOLUTION 1: Handle based on entity state
                if (isNewEntity || entity == null || entity.getId() == null) {
                    // For new entities, attachment is in pending list
                    Notification.show("Attachment updated | បានធ្វើបច្ចុប្បន្នភាព",
                        2200, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                } else {
                    // For existing entities, save immediately
                    attachmentRepository.save(attachment);
                    Notification.show("Attachment updated | បានធ្វើបច្ចុប្បន្នភាព",
                        2200, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }

                dialog.close();
                loadAttachments();
                onAttachmentChange.run();

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(),
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });
        btnSave.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        Button btnCancel = new Button("Cancel | បោះបង់", VaadinIcon.CLOSE.create(), e -> dialog.close());
        btnCancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(btnCancel, btnSave);
        dialog.add(form);
        dialog.open();
    }

    private Component buildFileInfoPreview(String fileName, String mimeType, long fileSize,
                                          List<Attachment> attachmentsList, Attachment attachment,
                                          VerticalLayout previewLayout) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setMargin(false);
        layout.setWidthFull();
        layout.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("border", "1px solid var(--lumo-contrast-20pct)");

        Icon icon = getFileIcon(mimeType);
        icon.setSize("40px");
        icon.getStyle().set("color", "var(--lumo-primary-color)");

        String displayName = fileName;
        if (displayName != null && displayName.contains("_")) {
            String[] parts = displayName.split("_", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                displayName = parts[1];
            }
        }

        Span nameSpan = new Span(displayName);
        nameSpan.getStyle().set("font-weight", "500").set("flex", "1");

        Span sizeSpan = new Span(FileSizeFormatter.formatBytes(fileSize));
        sizeSpan.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "0.875rem");

        if (attachment != null && attachment.getRemark() != null && !attachment.getRemark().isEmpty()) {
            Span remarkSpan = new Span(VaadinIcon.COMMENT.create());
            remarkSpan.getElement().setAttribute("title", attachment.getRemark());
            remarkSpan.getStyle()
                .set("color", "var(--lumo-primary-color-50pct)")
                .set("margin-left", "var(--lumo-space-s)");
            layout.add(remarkSpan);
        }

        if (attachmentsList != null && attachment != null) {
            Button removeBtn = new Button(VaadinIcon.TRASH.create(), ev -> {
                try {
                    Path filePath = uploadDir.resolve(fileName);
                    Files.deleteIfExists(filePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                attachmentsList.remove(attachment);
                previewLayout.remove(layout);
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            removeBtn.setTooltipText("Remove | ដកចេញ");
            layout.add(removeBtn);
        }

        layout.add(icon, nameSpan, sizeSpan);
        layout.setFlexGrow(1, nameSpan);
        
        return layout;
    }

    private Component buildFilePreview(String fileName, String mimeType, byte[] bytes,
                                       List<Attachment> attachmentsList, Attachment attachment,
                                       VerticalLayout previewLayout) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setMargin(false);
        layout.setWidthFull();
        layout.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("border", "1px solid var(--lumo-contrast-20pct)");

        Component previewContent;
        final byte[] finalBytes = bytes;
        
        if (mimeType != null && mimeType.startsWith("image/")) {
            if (bytes.length < 5 * 1024 * 1024) {
                Image thumb = new Image();
                thumb.setSrc("data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(finalBytes));
                thumb.setAlt(fileName);
                thumb.setMaxWidth("80px");
                thumb.setMaxHeight("60px");
                thumb.getStyle().set("object-fit", "cover");
                previewContent = thumb;
            } else {
                Icon imageIcon = VaadinIcon.PICTURE.create();
                imageIcon.setSize("40px");
                imageIcon.getStyle().set("color", "var(--lumo-primary-color)");
                previewContent = imageIcon;
            }
        } else if (mimeType != null && mimeType.equals("application/pdf")) {
            Icon pdfIcon = VaadinIcon.FILE_PRESENTATION.create();
            pdfIcon.setSize("40px");
            pdfIcon.getStyle().set("color", "var(--lumo-primary-color)");
            pdfIcon.getStyle().set("cursor", "pointer");
            pdfIcon.setTooltipText("Click to preview PDF");
            pdfIcon.addClickListener(e -> {
                Attachment tempAttachment = new Attachment();
                tempAttachment.setFileName(fileName);
                tempAttachment.setContentType(mimeType);
                tempAttachment.setFileData(finalBytes);
                previewAttachment(tempAttachment);
            });
            previewContent = pdfIcon;
        } else {
            Icon icon = getFileIcon(mimeType);
            icon.setSize("40px");
            icon.getStyle().set("color", "var(--lumo-primary-color)");
            previewContent = icon;
        }

        String displayName = fileName;
        if (displayName != null && displayName.contains("_")) {
            String[] parts = displayName.split("_", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                displayName = parts[1];
            }
        }

        Span nameSpan = new Span(displayName);
        nameSpan.getStyle().set("font-weight", "500").set("flex", "1");

        Span sizeSpan = new Span(FileSizeFormatter.formatBytes((long) bytes.length));
        sizeSpan.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "0.875rem");

        if (attachment != null && attachment.getRemark() != null && !attachment.getRemark().isEmpty()) {
            Span remarkSpan = new Span(VaadinIcon.COMMENT.create());
            remarkSpan.getElement().setAttribute("title", attachment.getRemark());
            remarkSpan.getStyle()
                .set("color", "var(--lumo-primary-color-50pct)")
                .set("margin-left", "var(--lumo-space-s)");
            layout.add(remarkSpan);
        }

        if (attachmentsList != null && attachment != null) {
            Button removeBtn = new Button(VaadinIcon.TRASH.create(), ev -> {
                try {
                    Path filePath = uploadDir.resolve(fileName);
                    Files.deleteIfExists(filePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                attachmentsList.remove(attachment);
                previewLayout.remove(layout);
            });
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            removeBtn.setTooltipText("Remove | ដកចេញ");
            layout.add(removeBtn);
        }

        layout.add(previewContent, nameSpan, sizeSpan);
        layout.setFlexGrow(1, nameSpan);
        
        return layout;
    }

    private void previewAttachment(Attachment attachment) {
        if (attachment == null) return;

        final Path filePath = uploadDir.resolve(attachment.getFileName());

        final Dialog previewDialog = new Dialog();

        // --- helper: remove UUID_ prefix from filename for display
        java.util.function.Function<String, String> displayNameOf = (fn) -> {
            if (fn == null) return "Unnamed";
            if (fn.contains("_")) {
                String[] parts = fn.split("_", 2);
                if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) return parts[1];
            }
            return fn;
        };

        // ✅ Delay setting iframe src/srcdoc until dialog is opened, and cancel load on close
        java.util.function.BiConsumer<IFrame, Runnable> setIFrameSourceWhenOpened =
                (iframe, setSource) -> {
                    final boolean[] loaded = {false};
                    previewDialog.addOpenedChangeListener(ev -> {
                        if (ev.isOpened()) {
                            if (!loaded[0]) {
                                loaded[0] = true;
                                setSource.run(); // set src/srcdoc only once
                            }
                        } else {
                            // ✅ important: cancel network load when closing
                            //iframe.setSrc("about:blank");
                        	iframe.getElement().removeAttribute("src");
                            iframe.getElement().removeAttribute("srcdoc");
                        }
                    });
                };

        // --- basic validations
        if (!Files.exists(filePath)) {
            Notification.show("File not found | រកមិនឃើញឯកសារ",
                    2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        final String mimeType = (attachment.getContentType() != null)
                ? attachment.getContentType()
                : "application/octet-stream";

        // --- file size warning
        long fileSize = -1;
        try {
            fileSize = Files.size(filePath);
            long warnAt = 100L * 1024 * 1024; // 100MB
            if (fileSize > warnAt) {
                Notification.show(
                        "File is very large (" + FileSizeFormatter.formatBytes(fileSize)
                                + "). Preview may be slow. Consider downloading instead.",
                        5000, Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        } catch (IOException ignored) { }

        // --- dialog setup
        final String displayName = displayNameOf.apply(attachment.getFileName());
        previewDialog.setHeaderTitle("Preview: " + displayName);
        previewDialog.setCloseOnEsc(true);
        previewDialog.setCloseOnOutsideClick(true);

        previewDialog.setWidth("95vw");
        previewDialog.setHeight("95vh");
        previewDialog.setMinWidth("1200px");
        previewDialog.setMinHeight("800px");

        final VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setMargin(false);
        content.setSizeFull();
        content.setAlignItems(FlexComponent.Alignment.STRETCH);

        // ====== IMAGE ======
        if (mimeType.startsWith("image/")) {
            StreamRegistration reg = null;
            try {
                String resourceId = "image-" + UUID.randomUUID() + ".bin";

                StreamResource resource = new StreamResource(resourceId, () -> {
                    try {
                        return Files.newInputStream(filePath);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading image file", e);
                    }
                });
                resource.setContentType(mimeType);
                resource.setCacheTime(0);

                reg = VaadinSession.getCurrent()
                        .getResourceRegistry()
                        .registerResource(resource);

                final String resourceUrl = reg.getResourceUri().toString();

                String imageViewerHtml = String.format(
                        "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "  <style>\n" +
                        "    *{margin:0;padding:0;box-sizing:border-box;}\n" +
                        "    html,body{width:100%%;height:100%%;overflow:hidden;}\n" +
                        "    body{font-family:Arial,sans-serif;display:flex;flex-direction:column;}\n" +
                        "    #toolbar{padding:10px;background:#f0f0f0;border-bottom:1px solid #ccc;display:flex;gap:10px;align-items:center;flex-wrap:wrap;}\n" +
                        "    #viewer-container{flex:1;overflow:auto;background:#525659;display:flex;justify-content:center;align-items:flex-start;padding:20px;}\n" +
                        "    #image-display{box-shadow:0 4px 12px rgba(0,0,0,0.3);background:white;transition:transform .1s ease;max-width:none;max-height:none;width:auto;height:auto;}\n" +
                        "    button,select{padding:5px 10px;border:1px solid #ccc;border-radius:4px;background:white;cursor:pointer;}\n" +
                        "    button:hover{background:#e0e0e0;}\n" +
                        "    #image-info{margin-left:auto;color:#666;}\n" +
                        "  </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <div id=\"toolbar\">\n" +
                        "    <button id=\"zoom-out\" title=\"Zoom Out\">−</button>\n" +
                        "    <select id=\"zoom-select\">\n" +
                        "      <option value=\"0.25\">25%%</option>\n" +
                        "      <option value=\"0.5\">50%%</option>\n" +
                        "      <option value=\"0.75\">75%%</option>\n" +
                        "      <option value=\"1\" selected>100%%</option>\n" +
                        "      <option value=\"1.25\">125%%</option>\n" +
                        "      <option value=\"1.5\">150%%</option>\n" +
                        "      <option value=\"2\">200%%</option>\n" +
                        "      <option value=\"3\">300%%</option>\n" +
                        "      <option value=\"4\">400%%</option>\n" +
                        "    </select>\n" +
                        "    <button id=\"zoom-in\" title=\"Zoom In\">+</button>\n" +
                        "    <button id=\"fit-width\" title=\"Fit to Width\">↔️</button>\n" +
                        "    <button id=\"fit-height\" title=\"Fit to Height\">↕️</button>\n" +
                        "    <button id=\"actual-size\" title=\"Actual Size\">🔍 1:1</button>\n" +
                        "    <span id=\"image-info\"><span id=\"dimensions\">Loading...</span></span>\n" +
                        "  </div>\n" +
                        "  <div id=\"viewer-container\">\n" +
                        "    <img id=\"image-display\" src=\"%s\" alt=\"Image Preview\">\n" +
                        "  </div>\n" +
                        "  <script>\n" +
                        "    const img=document.getElementById('image-display');\n" +
                        "    const container=document.getElementById('viewer-container');\n" +
                        "    const zoomSelect=document.getElementById('zoom-select');\n" +
                        "    const dimensionsSpan=document.getElementById('dimensions');\n" +
                        "    let currentScale=1.0; let naturalWidth=0; let naturalHeight=0;\n" +
                        "    img.onload=function(){naturalWidth=img.naturalWidth;naturalHeight=img.naturalHeight;dimensionsSpan.textContent=naturalWidth+' × '+naturalHeight+' px';updateImageSize();};\n" +
                        "    function updateImageSize(){img.style.width=(naturalWidth*currentScale)+'px';img.style.height='auto';}\n" +
                        "    function setZoom(scale){currentScale=scale;zoomSelect.value=scale.toString();updateImageSize();}\n" +
                        "    document.getElementById('zoom-in').addEventListener('click',()=>{const s=[0.25,0.5,0.75,1,1.25,1.5,2,3,4];let n=s.find(x=>x>currentScale)||4;setZoom(n);});\n" +
                        "    document.getElementById('zoom-out').addEventListener('click',()=>{const s=[0.25,0.5,0.75,1,1.25,1.5,2,3,4];let p=[...s].reverse().find(x=>x<currentScale)||0.25;setZoom(p);});\n" +
                        "    zoomSelect.addEventListener('change',e=>setZoom(parseFloat(e.target.value)));\n" +
                        "    document.getElementById('fit-width').addEventListener('click',()=>{const w=container.clientWidth-40;setZoom(w/naturalWidth);});\n" +
                        "    document.getElementById('fit-height').addEventListener('click',()=>{const h=container.clientHeight-40;setZoom(h/naturalHeight);});\n" +
                        "    document.getElementById('actual-size').addEventListener('click',()=>setZoom(1.0));\n" +
                        "  </script>\n" +
                        "</body>\n" +
                        "</html>",
                        resourceUrl
                );

                IFrame iframe = new IFrame();
                iframe.setSizeFull();
                iframe.getElement().setAttribute("style", "border:none;width:100%;height:100%;");
                // optional: start blank
                //iframe.setSrc("about:blank");
                setIFrameSourceWhenOpened.accept(iframe, () -> iframe.getElement().setAttribute("srcdoc", imageViewerHtml));

                VerticalLayout iframeContainer = new VerticalLayout(iframe);
                iframeContainer.setSizeFull();
                iframeContainer.setPadding(false);
                iframeContainer.setMargin(false);
                iframeContainer.setSpacing(false);

                content.add(iframeContainer);
                content.expand(iframeContainer);

                StreamRegistration finalReg = reg;
                previewDialog.addDetachListener(ev -> finalReg.unregister());

            } catch (Exception e) {
                if (reg != null) reg.unregister();
                content.add(new Span("Error loading image: " + e.getMessage()));
            }

        // ====== VIDEO ======
        } else if (mimeType.startsWith("video/")) {
            StreamRegistration reg = null;
            try {
                String resourceId = "video-" + UUID.randomUUID() + ".bin";

                StreamResource resource = new StreamResource(resourceId, () -> {
                    try {
                        return Files.newInputStream(filePath);
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading video file", e);
                    }
                });
                resource.setContentType(mimeType);
                resource.setCacheTime(0);

                reg = VaadinSession.getCurrent()
                        .getResourceRegistry()
                        .registerResource(resource);

                final String resourceUrl = reg.getResourceUri().toString();

                String videoPlayerHtml = String.format(
                        "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "  <style>\n" +
                        "    *{margin:0;padding:0;box-sizing:border-box;}\n" +
                        "    html,body{width:100%%;height:100%%;overflow:hidden;}\n" +
                        "    body{font-family:Arial,sans-serif;background:#525659;display:flex;justify-content:center;align-items:center;}\n" +
                        "    #video-container{width:100%%;height:100%%;display:flex;justify-content:center;align-items:center;padding:20px;}\n" +
                        "    video{max-width:100%%;max-height:100%%;box-shadow:0 4px 12px rgba(0,0,0,0.3);background:black;}\n" +
                        "  </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <div id=\"video-container\">\n" +
                        "    <video controls autoplay>\n" +
                        "      <source src=\"%s\" type=\"%s\" />\n" +
                        "      Your browser does not support the video tag.\n" +
                        "    </video>\n" +
                        "  </div>\n" +
                        "</body>\n" +
                        "</html>",
                        resourceUrl, mimeType
                );

                IFrame iframe = new IFrame();
                iframe.setSizeFull();
                iframe.getElement().setAttribute("style", "border:none;width:100%;height:100%;");
               // iframe.setSrc("about:blank");
                setIFrameSourceWhenOpened.accept(iframe, () -> iframe.getElement().setAttribute("srcdoc", videoPlayerHtml));

                VerticalLayout iframeContainer = new VerticalLayout(iframe);
                iframeContainer.setSizeFull();
                iframeContainer.setPadding(false);
                iframeContainer.setMargin(false);
                iframeContainer.setSpacing(false);

                content.add(iframeContainer);
                content.expand(iframeContainer);

                StreamRegistration finalReg = reg;
                previewDialog.addDetachListener(ev -> finalReg.unregister());

            } catch (Exception e) {
                if (reg != null) reg.unregister();
                content.add(new Span("Error loading video: " + e.getMessage()));
            }

        // ====== PDF ======
        } else if ("application/pdf".equals(mimeType)) {
            StreamRegistration reg = null;
            try {
                String resourceId = "pdf-" + UUID.randomUUID() + ".pdf";

                StreamResource resource = new StreamResource(resourceId, () -> {
                    try {
                        return Files.newInputStream(filePath);
                    } catch (IOException ex) {
                        throw new RuntimeException("Error reading PDF file", ex);
                    }
                });
                resource.setContentType("application/pdf");
                resource.setCacheTime(0);
                resource.setHeader("Content-Disposition", "inline");

                reg = VaadinSession.getCurrent()
                        .getResourceRegistry()
                        .registerResource(resource);

                final String rawUrl = reg.getResourceUri().toString();
                final String finalUrl = rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl;

                // ⭐ Optional: for big PDFs, prefer new tab instead of iframe
                // (helps avoid browser aborts / connection reset logs)
                if (fileSize > 100L * 1024 * 1024) { // 25MB
                    Notification.show("PDF is large. Opening in new tab is recommended.",
                            3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);

                    StreamRegistration finalReg = reg;
                    previewDialog.addDetachListener(ev -> finalReg.unregister());

                    // open new tab and close dialog
                    com.vaadin.flow.component.UI.getCurrent().getPage()
                            .executeJs("window.open($0, '_blank')", finalUrl);
                    return;
                }

                IFrame iframe = new IFrame();
                iframe.setSizeFull();
                iframe.getStyle().set("border", "none");
               // iframe.setSrc("about:blank");

                StreamRegistration finalReg = reg;
                setIFrameSourceWhenOpened.accept(iframe, () -> iframe.setSrc(finalUrl));

                VerticalLayout iframeContainer = new VerticalLayout(iframe);
                iframeContainer.setSizeFull();
                iframeContainer.setPadding(false);
                iframeContainer.setMargin(false);
                iframeContainer.setSpacing(false);

                content.add(iframeContainer);
                content.expand(iframeContainer);

                previewDialog.addDetachListener(ev -> finalReg.unregister());

            } catch (Exception e) {
                if (reg != null) reg.unregister();
                content.add(new Span("Error loading PDF: " + e.getMessage()));

                Button downloadBtn = new Button("Download PDF | ទាញយក PDF",
                        VaadinIcon.DOWNLOAD.create(), ev -> {
                    previewDialog.close();
                    downloadAttachment(attachment);
                });
                downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                content.add(downloadBtn);
            }

        // ====== OTHER ======
        } else {
            Button downloadBtn = new Button("Download File | ទាញយកឯកសារ", VaadinIcon.DOWNLOAD.create(), e -> {
                previewDialog.close();
                downloadAttachment(attachment);
            });
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            VerticalLayout nonPreviewLayout = new VerticalLayout(
                    new Span("Preview not available for this file type | មិនអាចមើលឯកសារប្រភេទនេះបានទេ"),
                    downloadBtn
            );
            nonPreviewLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            content.add(nonPreviewLayout);
        }

        // remark (bottom)
        if (attachment.getRemark() != null && !attachment.getRemark().isEmpty()) {
            Span remarkLabel = new Span("Remark | កំណត់សម្គាល់: " + attachment.getRemark());
            remarkLabel.getStyle()
                    .set("font-size", "0.875rem")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("background-color", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("width", "100%");
            content.add(remarkLabel);
        }

        // Footer
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footerLayout.setPadding(true);

        String downloadButtonText = "Download File | ទាញយកឯកសារ";
        if (attachment.getContentType() != null) {
            if (attachment.getContentType().startsWith("image/")) downloadButtonText = "Download Image | ទាញយករូបភាព";
            else if (attachment.getContentType().startsWith("video/")) downloadButtonText = "Download Video | ទាញយកវីដេអូ";
            else if ("application/pdf".equals(attachment.getContentType())) downloadButtonText = "Download PDF | ទាញយក PDF";
        }

        Button downloadFooterBtn = new Button(downloadButtonText, VaadinIcon.DOWNLOAD.create(),
                e -> downloadAttachment(attachment));
        downloadFooterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button closeBtn = new Button("Close | បិទ", e -> previewDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        footerLayout.add(downloadFooterBtn, closeBtn);

        previewDialog.add(content);
        previewDialog.getFooter().add(footerLayout);
        previewDialog.open();
    }



    // Helper method to get a download URL for the file
    private String getFileDownloadUrl(Attachment attachment) {
        try {
            String downloadFileName = attachment.getFileName();
            if (downloadFileName != null && downloadFileName.contains("_")) {
                String[] parts = downloadFileName.split("_", 2);
                if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                    downloadFileName = parts[1];
                }
            }
            
            StreamResource resource = new StreamResource(
                downloadFileName != null ? downloadFileName : "attachment_" + attachment.getId(),
                () -> {
                    try {
                        return Files.newInputStream(uploadDir.resolve(attachment.getFileName()));
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file", e);
                    }
                }
            );
            resource.setContentType(attachment.getContentType());
            
            StreamRegistration registration = VaadinSession.getCurrent()
                .getResourceRegistry()
                .registerResource(resource);
            
            return com.vaadin.flow.component.UI.getCurrent().getInternals()
                .getContextRootRelativePath() + registration.getResourceUri().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    private String getBrowserPdfViewerHtml(byte[] pdfData, String base64Data) {
        // Get user agent from current UI
        String userAgent = com.vaadin.flow.component.UI.getCurrent().getElement()
            .executeJs("return navigator.userAgent").toString();
        
        boolean isChrome = userAgent.contains("Chrome");
        boolean isFirefox = userAgent.contains("Firefox");
        boolean isEdge = userAgent.contains("Edg");
        boolean isSafari = userAgent.contains("Safari") && !isChrome;
        
        if (isChrome || isEdge) {
            // Chrome/Edge work best with object tag
            return String.format(
                "<object data='data:application/pdf;base64,%s' type='application/pdf' width='100%%' height='550px'>" +
                "<p>PDF cannot be displayed. <a href='#' onclick='window.open(\"data:application/pdf;base64,%s\")'>Download</a></p>" +
                "</object>",
                base64Data, base64Data
            );
        } else if (isFirefox) {
            // Firefox works better with iframe
            return String.format(
                "<iframe src='data:application/pdf;base64,%s' width='100%%' height='550px' style='border: none;'></iframe>",
                base64Data
            );
        } else {
            // For other browsers, use embed
            return String.format(
                "<embed src='data:application/pdf;base64,%s' type='application/pdf' width='100%%' height='550px' />",
                base64Data
            );
        }
    }
    
 
    private void downloadAttachment(Attachment attachment) {
        if (attachment == null) return;

        Path filePath = uploadDir.resolve(attachment.getFileName());
        
        if (!Files.exists(filePath)) {
            Notification.show("File not found | រកមិនឃើញឯកសារ",
                2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String downloadFileName = attachment.getFileName();
        if (downloadFileName != null && downloadFileName.contains("_")) {
            String[] parts = downloadFileName.split("_", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F-]{36}")) {
                downloadFileName = parts[1];
            }
        }

        StreamResource resource = new StreamResource(
            downloadFileName != null ? downloadFileName : "attachment_" + attachment.getId(),
            () -> {
                try {
                    return Files.newInputStream(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Error reading file for download", e);
                }
            }
        );

        if (attachment.getContentType() != null) {
            resource.setContentType(attachment.getContentType());
        }

        StreamRegistration registration = VaadinSession.getCurrent()
            .getResourceRegistry()
            .registerResource(resource);
        
        String resourceUrl = registration.getResourceUri().toASCIIString();
        
        com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
            "window.open($0, '_blank')", resourceUrl
        );
    }

    /**
     * Confirm and delete attachment - Only called in edit mode
     */
    private void confirmDelete(Attachment attachment) {
        // Prevent delete in view-only mode
        if (viewOnlyMode) {
            return;
        }
        
        ConfirmDialog confirm = new ConfirmDialog();
        confirm.setHeader("Confirm Delete | បញ្ជាក់ការលុប");
        confirm.setText("Are you sure you want to delete this attachment? | តើអ្នកប្រាកដថាចង់លុបឯកសារនេះទេ?");
        confirm.setCancelable(true);
        confirm.setCancelText("Cancel | បោះបង់");
        confirm.setConfirmText("Delete | លុប");
        confirm.setConfirmButtonTheme("error primary");

        confirm.addConfirmListener(e -> {
            try {
                // Delete file from disk
                Path filePath = uploadDir.resolve(attachment.getFileName());
                Files.deleteIfExists(filePath);

                // SOLUTION 1: Handle based on entity state
                if (isNewEntity || entity == null || entity.getId() == null) {
                    // New entity - remove from pending list
                    pendingAttachments.remove(attachment);
                } else {
                    // Existing entity - delete from database
                    attachmentRepository.delete(attachment);
                }
                
                attachments.remove(attachment);

                Notification.show("Attachment deleted | បានលុបឯកសារ",
                    2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                grid.setItems(attachments);
                onAttachmentChange.run();

            } catch (Exception ex) {
                Notification.show("Error deleting attachment: " + ex.getMessage(),
                    3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });

        confirm.open();
    }

    /**
     * Update the entity reference
     */
    public void setEntity(AbstractEntity newEntity) {
        this.entity = newEntity;
        loadAttachments();
    }

    /**
     * Refresh attachments from database or pending list
     */
    public void refresh() {
        loadAttachments();
    }

    /**
     * Get current attachments list
     */
    public List<Attachment> getAttachments() {
        return new ArrayList<>(attachments);
    }

    /**
     * Clear all attachments from grid (does not delete from DB)
     */
    public void clear() {
        attachments.clear();
        pendingAttachments.clear();
        grid.setItems(attachments);
        grid.getDataProvider().refreshAll();
    }
}
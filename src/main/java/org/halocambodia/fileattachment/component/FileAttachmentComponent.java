package org.halocambodia.fileattachment.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.halocambodia.fileattachment.component.event.FileAttachmentChangedEvent;
import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.halocambodia.data.AttachmentType;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.fileattachment.service.FileAttachmentService;
import org.halocambodia.upload.component.FileThumbnail;
import org.halocambodia.upload.component.LargeFileUploader;
import org.halocambodia.upload.domain.UploadType;
import org.halocambodia.utility.ApplicationUrlUtil;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.shared.Registration;

/**
 * Reusable new disk-based file attachment management component. It is not tied to a Tab and may be
 * placed in any Vaadin layout.
 */
public class FileAttachmentComponent extends VerticalLayout {

    private static final long DEFAULT_CHUNK_SIZE = 20L * 1024L * 1024L;
    private static final long DEFAULT_MAX_FILE_SIZE = 20L * 1024L * 1024L * 1024L;

    private final FileAttachmentService attachmentService;
    private final AttachmentTypeRepository attachmentTypeRepository;
    private final FileAttachmentOwnerType ownerType;
    private final UploadType uploadType;

    private final LargeFileUploader uploader = new LargeFileUploader();
    private final Grid<FileAttachment> grid = new Grid<>(FileAttachment.class, false);

    private final Button editSaveButton = new Button("Edit", VaadinIcon.EDIT.create());
   // private final Button downloadButton = new Button("Download", VaadinIcon.DOWNLOAD.create());
   // private final Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());
   // private final Button propertiesButton = new Button("Properties", VaadinIcon.INFO_CIRCLE.create());
    private final TextField searchField = new TextField();

    private final Span uploadNote = new Span();
    private final Span stateBadge = new Span("Saved");

    private final List<FileAttachment> attachments = new ArrayList<>();
    private final Set<Long> deletedIds = new LinkedHashSet<>();
    private final List<FileAttachment> draggedAttachments = new ArrayList<>();
    private final List<AttachmentType> attachmentTypes = new ArrayList<>();

    private final Map<FileAttachment, Boolean> editState = new HashMap<>();
    private final Map<FileAttachment, ComboBox<AttachmentType>> typeEditors = new HashMap<>();
    private final Map<FileAttachment, TextArea> descriptionEditors = new HashMap<>();
    private final Map<FileAttachment, IntegerField> orderEditors = new HashMap<>();

    private Long ownerId;
    private boolean pendingChanges;
    private boolean attachmentTypeRequired = true;

    public FileAttachmentComponent(
            FileAttachmentService attachmentService,
            AttachmentTypeRepository attachmentTypeRepository,
            FileAttachmentOwnerType ownerType,
            UploadType uploadType) {
        this(
            attachmentService,
            attachmentTypeRepository,
            ownerType,
            uploadType,
            "Upload one or more files."
        );
    }

    public FileAttachmentComponent(
            FileAttachmentService attachmentService,
            AttachmentTypeRepository attachmentTypeRepository,
            FileAttachmentOwnerType ownerType,
            UploadType uploadType,
            String note) {

        this.attachmentService = attachmentService;
        this.attachmentTypeRepository = attachmentTypeRepository;
        this.ownerType = ownerType;
        this.uploadType = uploadType != null ? uploadType : UploadType.OTHER;

        setPadding(false);
        setSpacing(true);
        setWidthFull();

        uploadNote.setText(note != null ? note : "");
        uploadNote.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)");

        configureUploader();
        configureToolbar();
        configureGrid();
        loadAttachmentTypes();

        add(uploadNote, uploader, buildToolbar(), grid);
        refreshIndicators();
    }

    private void configureUploader() {
        uploader.setUploadType(uploadType);
        uploader.setMaxFiles(10);
        uploader.setRequestedChunkSize(DEFAULT_CHUNK_SIZE);
        uploader.setMaxFileSize(DEFAULT_MAX_FILE_SIZE);
        uploader.setVerifyChunkChecksum(false);
        uploader.setDisabled(false);
        uploader.setWidthFull();

        uploader.addStartedListener(event -> Notification.show(
            "Uploading: " + event.getFileName(),
            2000,
            Notification.Position.BOTTOM_START
        ));

        uploader.addCompletedListener(event -> {
            UUID uploadId = event.getUploadId();
            boolean duplicate = attachments.stream()
                .anyMatch(item -> uploadId.equals(item.getFileUuid()));
            if (duplicate) {
                return;
            }

            FileAttachment attachment = new FileAttachment();
            attachment.setOwnerType(ownerType);
            attachment.setOwnerId(ownerId);
            attachment.setUploadType(uploadType);
            attachment.setFileUuid(uploadId);
            attachment.setFileName(event.getOriginalFileName());
            attachment.setMimeType(
                event.getMimeType() == null || event.getMimeType().isBlank()
                    ? "application/octet-stream"
                    : event.getMimeType()
            );
            attachment.setFileSize(event.getFileSize());
            attachment.setChecksumSha256(event.getChecksumSha256());
            attachment.setDescription("");
            attachment.setSortOrder(attachments.size() + 1);

            attachments.add(attachment);
            editState.put(attachment, true);
            markChanged();
            refreshGrid();
            grid.deselectAll();
            grid.select(attachment);
        });

        uploader.addFailedListener(event -> Notification.show(
            "Upload failed: " + event.getMessage(),
            5000,
            Notification.Position.MIDDLE
        ));
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout actions = new HorizontalLayout(
            editSaveButton
        );
        actions.setPadding(false);
        actions.setMargin(false);
        actions.setSpacing(true);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout statuses = new HorizontalLayout(
            stateBadge
        );
        statuses.setPadding(false);
        statuses.setMargin(false);
        statuses.setSpacing(true);
        statuses.setAlignItems(FlexComponent.Alignment.CENTER);

        searchField.setPlaceholder("Search attachments... | ស្វែងរកឯកសារ");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("280px");
        searchField.addValueChangeListener(event -> refreshGrid());

        HorizontalLayout left = new HorizontalLayout(actions, searchField);
        left.setPadding(false);
        left.setMargin(false);
        left.setSpacing(true);
        left.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout toolbar = new HorizontalLayout(left, statuses);
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setMargin(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void configureToolbar() {

        editSaveButton.addClickListener(event -> handleEditSave());

        styleBadge(stateBadge);
        updateToolbarState();
    }

    private void downloadSelected() {
        Set<FileAttachment> selected =
            new LinkedHashSet<>(grid.getSelectedItems());

        if (selected.isEmpty()) {
            Notification.show(
                "Please select at least one file. | សូមជ្រើសរើសឯកសារយ៉ាងហោចណាស់មួយ",
                3000,
                Notification.Position.MIDDLE
            );
            return;
        }

        List<FileAttachment> downloadable = selected.stream()
            .filter(java.util.Objects::nonNull)
            .filter(item -> item.getFileUuid() != null)
            .toList();

        if (downloadable.isEmpty()) {
            Notification.show(
                "The selected file is not available for download.",
                3000,
                Notification.Position.MIDDLE
            );
            return;
        }

        if (downloadable.size() == 1) {
            openDownload(downloadable.getFirst());
            return;
        }

        downloadSelectedAsZip();
        return;
    }
    
    private void configureToolbarButton(Button button, String tooltip) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.getElement().setProperty("title", tooltip);
        button.getElement().setAttribute("aria-label", tooltip);
        button.getStyle().set("margin", "0").set("white-space", "nowrap");
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.setRowsDraggable(true);
        grid.setDropMode(GridDropMode.BETWEEN);




        grid.addComponentColumn(this::buildFileCell)
            .setHeader("File Name | ឈ្មោះឯកសារ")
            .setAutoWidth(true)
            .setFlexGrow(0)
            .setResizable(true);
        
        grid.addComponentColumn(this::buildTypeCell)
        .setHeader("FileAttachment Type | ប្រភេទឯកសារភ្ជាប់")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setResizable(true);

        grid.addComponentColumn(this::buildDescriptionCell)
            .setHeader("Description | ការពិពណ៌នា")
            .setFlexGrow(1)
            .setResizable(true);
        
        grid.addColumn(item -> formatFileSize(item.getFileSize()))
        .setHeader("Size | ទំហំ")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setResizable(true);
        

        grid.addComponentColumn(this::buildOrderCell)
            .setHeader("Order | លំដាប់")
            .setAutoWidth(true)
            .setFlexGrow(0)
            .setResizable(true);

        grid.addSelectionListener(event -> updateToolbarState());
        grid.addDragStartListener(event -> {
            draggedAttachments.clear();
            draggedAttachments.addAll(event.getDraggedItems());
        });
        grid.addDragEndListener(event -> draggedAttachments.clear());
        grid.addDropListener(event -> reorder(
            draggedAttachments,
            event.getDropTargetItem().orElse(null),
            event.getDropLocation()
        ));

        configureContextMenu();
        refreshGrid();
    }



    private void configureContextMenu() {
        GridContextMenu<FileAttachment> menu = grid.addContextMenu();
        menu.addItem("Preview | មើលជាមុន", event -> event.getItem().ifPresent(this::openPreview));
        menu.addItem("Download | ទាញយក", event -> downloadSelected());
        menu.addItem("Properties | ព័ត៌មានឯកសារ", event -> event.getItem().ifPresent(this::showProperties));
        menu.addItem("Edit | កែប្រែ", event -> event.getItem().ifPresent(item -> {
            grid.deselectAll();
            grid.select(item);
            if (!isEditing(item)) {
                editState.put(item, true);
                grid.getDataProvider().refreshItem(item);
            }
            updateToolbarState();
        }));
        menu.addItem("Delete | លុប", event ->deleteSelected());
    }

    private Component buildFileCell(FileAttachment attachment) {

        HorizontalLayout layout = new HorizontalLayout();
        layout.setPadding(false);
        layout.setMargin(false);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Component thumbnailOrIcon = buildThumbnailOrIcon(attachment);

        Span fileName = new Span(safeFileName(attachment));
        fileName.getElement().setAttribute(
            "title",
            "Click to preview | ចុចដើម្បីមើលជាមុន"
        );

        fileName.getStyle()
            .set("cursor", "pointer")
            .set("color", "var(--lumo-primary-text-color)")
            .set("text-decoration", "underline")
            .set("font-weight", "500")
            .set("white-space", "normal")
            .set("word-break", "break-word");

        fileName.addClickListener(event -> openPreview(attachment));

        layout.add(thumbnailOrIcon, fileName);
        layout.setFlexGrow(0, thumbnailOrIcon);
        layout.setFlexGrow(1, fileName);

        return layout;
    }

    private Component buildThumbnailOrIcon(FileAttachment attachment) {

        if (attachment == null || attachment.getFileUuid() == null) {
            return buildFileIconContainer(attachment);
        }

        if (!supportsThumbnail(attachment)) {
            return buildFileIconContainer(attachment);
        }

        FileThumbnail thumbnail = new FileThumbnail();
        thumbnail.setUploadUuid(attachment.getFileUuid());
        thumbnail.setWidth("48px");
        thumbnail.setHeight("48px");

        thumbnail.getStyle()
            .set("min-width", "48px")
            .set("min-height", "48px")
            .set("border-radius", "6px")
            .set("overflow", "hidden")
            .set("cursor", "pointer")
            .set("background", "var(--lumo-contrast-5pct)");

        thumbnail.getElement().setAttribute(
            "title",
            "Click to preview | ចុចដើម្បីមើលជាមុន"
        );

        thumbnail.addClickListener(event -> openPreview(attachment));

        return thumbnail;
    }
    
    private boolean supportsThumbnail(FileAttachment attachment) {

        if (attachment == null) {
            return false;
        }

        String mimeType = attachment.getMimeType() == null
            ? ""
            : attachment.getMimeType().toLowerCase(Locale.ROOT);

        String fileName = safeFileName(attachment)
            .toLowerCase(Locale.ROOT);

        return mimeType.startsWith("image/")
            || mimeType.startsWith("video/")
            || "application/pdf".equals(mimeType)
            || fileName.endsWith(".pdf");
    }
    private Component buildFileIconContainer(FileAttachment attachment) {

        Icon icon = fileIcon(attachment);
        icon.setSize("28px");
        icon.getStyle()
            .set("color", "var(--lumo-primary-color)");

        Div container = new Div(icon);
        container.setWidth("48px");
        container.setHeight("48px");

        container.getStyle()
            .set("min-width", "48px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("border-radius", "6px")
            .set("background", "var(--lumo-contrast-5pct)");

        return container;
    }
    

    
    private Component buildTypeCell(FileAttachment attachment) {
        if (!isEditing(attachment)) {
            if (attachment.getAttachmentType() == null) {
                Span missing = new Span("Not selected | មិនទាន់បានជ្រើស");
                missing.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
                    .set("font-style", "italic");
                return missing;
            }
            return new Span(attachment.getAttachmentType().getAttachmentTypeName());
        }

        ComboBox<AttachmentType> field = new ComboBox<>();
        field.setItems(attachmentTypes);
        field.setItemLabelGenerator(AttachmentType::getAttachmentTypeName);
        field.setPlaceholder("Select type | ជ្រើសរើសប្រភេទ");
        field.setClearButtonVisible(!attachmentTypeRequired);
        field.setRequiredIndicatorVisible(attachmentTypeRequired);
        field.setWidthFull();
        field.setValue(attachment.getAttachmentType());
        typeEditors.put(attachment, field);
        return field;
    }

    private Component buildDescriptionCell(FileAttachment attachment) {
        if (!isEditing(attachment)) {
            String value = attachment.getDescription();
            Span text = new Span(value == null || value.isBlank() ? "-" : value);
            text.getStyle().set("white-space", "normal").set("word-break", "break-word");
            return text;
        }

        TextArea field = new TextArea();
        field.setWidthFull();
        field.setMinHeight("72px");
        field.setMaxLength(2000);
        field.setValue(attachment.getDescription() != null ? attachment.getDescription() : "");
        descriptionEditors.put(attachment, field);
        return field;
    }

    private Component buildOrderCell(FileAttachment attachment) {
        if (!isEditing(attachment)) {
            return new Span(String.valueOf(attachment.getSortOrder() != null ? attachment.getSortOrder() : 0));
        }

        IntegerField field = new IntegerField();
        field.setWidth("95px");
        field.setMin(0);
        field.setStepButtonsVisible(true);
        field.setValue(attachment.getSortOrder() != null ? attachment.getSortOrder() : 0);
        orderEditors.put(attachment, field);
        return field;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
        clearEditorState();
        deletedIds.clear();
        pendingChanges = false;
        attachments.clear();
        attachments.addAll(attachmentService.findByOwner(ownerType, ownerId));
        refreshGrid();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void reload() {
        setOwnerId(ownerId);
    }

    public void saveChanges(Long persistedOwnerId) {
        if (persistedOwnerId == null) {
            throw new IllegalArgumentException("Save the parent record before saving attachments");
        }
        commitAllPendingEdits();
        validateAttachments();

        List<FileAttachment> saved = attachmentService.saveOwnerAttachments(
            ownerType,
            persistedOwnerId,
            attachments,
            deletedIds
        );

        this.ownerId = persistedOwnerId;
        attachments.clear();
        attachments.addAll(saved);
        deletedIds.clear();
        clearEditorState();
        pendingChanges = false;
        refreshGrid();
    }

    public void validateAttachments() {
        for (FileAttachment attachment : attachments) {
            if (attachmentTypeRequired && attachment.getAttachmentType() == null) {
                throw new IllegalArgumentException(
                    "FileAttachment type is required for file: " + safeFileName(attachment)
                );
            }
        }
    }

    public void commitAllPendingEdits() {
        List<FileAttachment> editing = editState.entrySet().stream()
            .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .toList();
        for (FileAttachment attachment : editing) {
            commitEdit(attachment, false);
        }
    }

    public boolean hasPendingChanges() {
        return pendingChanges;
    }

    public List<FileAttachment> getAttachments() {
        return List.copyOf(attachments);
    }

    public void setAttachmentTypeRequired(boolean required) {
        this.attachmentTypeRequired = required;
        refreshGrid();
    }

    public void setUploadNote(String note) {
        uploadNote.setText(note != null ? note : "");
    }

    public LargeFileUploader getUploader() {
        return uploader;
    }

    public Grid<FileAttachment> getGrid() {
        return grid;
    }

    public Registration addChangedListener(
            ComponentEventListener<FileAttachmentChangedEvent> listener) {
        return addListener(FileAttachmentChangedEvent.class, listener);
    }

    private void handleEditSave() {
        List<FileAttachment> selected = new ArrayList<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        List<FileAttachment> editing = selected.stream()
            .filter(this::isEditing)
            .toList();

        if (editing.isEmpty()) {
            // Put every selected row into edit mode at the same time.
            selected.forEach(attachment -> editState.put(attachment, true));
            grid.getDataProvider().refreshAll();
            updateToolbarState();
            return;
        }

        // Validate the whole selection before changing any row. This prevents a
        // partial save where some selected rows are committed and another fails.
        editing.forEach(this::validatePendingEdit);
        editing.forEach(this::applyPendingEditValues);
        sortAndRenumber();
        markChanged();

        Notification.show(
            editing.size() + " attachment(s) saved in the form. "
                + "Save the parent record to persist them.",
            3000,
            Notification.Position.MIDDLE
        );

        grid.getDataProvider().refreshAll();
        updateToolbarState();
    }

    private void validatePendingEdit(FileAttachment attachment) {
        if (!attachmentTypeRequired) {
            return;
        }

        ComboBox<AttachmentType> typeField = typeEditors.get(attachment);
        AttachmentType selectedType = typeField != null
            ? typeField.getValue()
            : attachment.getAttachmentType();

        if (selectedType == null) {
            throw new IllegalArgumentException(
                "FileAttachment type is required for file: " + safeFileName(attachment)
            );
        }
    }

    private void commitEdit(FileAttachment attachment, boolean notify) {
        validatePendingEdit(attachment);
        applyPendingEditValues(attachment);
        sortAndRenumber();
        markChanged();
        if (notify) {
            Notification.show(
                "FileAttachment changes saved in the form. Save the parent record to persist them.",
                3000,
                Notification.Position.MIDDLE
            );
        }
    }

    private void applyPendingEditValues(FileAttachment attachment) {
        ComboBox<AttachmentType> typeField = typeEditors.get(attachment);
        if (typeField != null) {
            attachment.setAttachmentType(typeField.getValue());
        }

        TextArea descriptionField = descriptionEditors.get(attachment);
        if (descriptionField != null) {
            attachment.setDescription(descriptionField.getValue());
        }

        IntegerField orderField = orderEditors.get(attachment);
        if (orderField != null) {
            attachment.setSortOrder(
                orderField.getValue() != null ? orderField.getValue() : 0
            );
        }

        editState.remove(attachment);
        typeEditors.remove(attachment);
        descriptionEditors.remove(attachment);
        orderEditors.remove(attachment);
    }

    private void deleteSelected() {
        Set<FileAttachment> selected = new LinkedHashSet<>(grid.getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        for (FileAttachment attachment : selected) {
            if (attachment.getId() != null) {
                deletedIds.add(attachment.getId());
            }
            attachments.remove(attachment);
            editState.remove(attachment);
            typeEditors.remove(attachment);
            descriptionEditors.remove(attachment);
            orderEditors.remove(attachment);
        }

        grid.deselectAll();
        sortAndRenumber();
        markChanged();
    }

    private void reorder(
            Collection<FileAttachment> dragged,
            FileAttachment target,
            GridDropLocation location) {
        if (dragged == null || dragged.isEmpty()) {
            return;
        }

        List<FileAttachment> moving = new ArrayList<>(dragged);
        attachments.removeAll(moving);

        int targetIndex = target == null ? attachments.size() : attachments.indexOf(target);
        if (targetIndex < 0) {
            targetIndex = attachments.size();
        } else if (location == GridDropLocation.BELOW) {
            targetIndex++;
        }

        attachments.addAll(targetIndex, moving);
        sortAndRenumberByCurrentOrder();
        markChanged();
    }

    private void sortAndRenumber() {
        attachments.sort(
            Comparator.comparing(
                FileAttachment::getSortOrder,
                Comparator.nullsLast(Integer::compareTo)
            ).thenComparing(
                FileAttachment::getFileName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            )
        );
        sortAndRenumberByCurrentOrder();
    }

    private void sortAndRenumberByCurrentOrder() {
        for (int index = 0; index < attachments.size(); index++) {
            attachments.get(index).setSortOrder(index + 1);
        }
        refreshGrid();
    }

    private Optional<FileAttachment> singleSelected() {
        Set<FileAttachment> selected = grid.getSelectedItems();
        return selected.size() == 1 ? selected.stream().findFirst() : Optional.empty();
    }

    private void updateToolbarState() {
        Set<FileAttachment> selected = grid.getSelectedItems();
        boolean any = !selected.isEmpty();
        editSaveButton.setEnabled(any);

        long editingCount = selected.stream()
            .filter(this::isEditing)
            .count();

        boolean saving = editingCount > 0;
        if (saving) {
            editSaveButton.setText(
                selected.size() > 1 ? "Save Selected (" + editingCount + ")" : "Save"
            );
            editSaveButton.setIcon(VaadinIcon.CHECK.create());
            editSaveButton.getElement().setProperty(
                "title",
                "Save changes for all selected rows currently in edit mode"
            );
        } else {
            editSaveButton.setText(
                selected.size() > 1 ? "Edit Selected (" + selected.size() + ")" : "Edit"
            );
            editSaveButton.setIcon(VaadinIcon.EDIT.create());
            editSaveButton.getElement().setProperty(
                "title",
                "Edit all selected attachment rows"
            );
        }
    }

    private boolean isEditing(FileAttachment attachment) {
        return Boolean.TRUE.equals(editState.get(attachment));
    }

    private void openDownload(FileAttachment attachment) {
        if (attachment.getFileUuid() == null) {
            return;
        }
        String url = ApplicationUrlUtil.contextUrl(
            "/api/uploads/" + attachment.getFileUuid() + "/download"
        );
        UI.getCurrent().getPage().open(url, "_blank");
    }

    private void openPreview(FileAttachment attachment) {
        if (attachment.getFileUuid() == null) {
            return;
        }

        String mime = attachment.getMimeType() != null
            ? attachment.getMimeType()
            : "application/octet-stream";
        String url = ApplicationUrlUtil.contextUrl(
            "/api/uploads/" + attachment.getFileUuid() + "/preview"
        );

        if ("application/pdf".equalsIgnoreCase(mime)) {
            IFrame frame = new IFrame(url);
            frame.setWidth("100%");
            frame.setHeight("78vh");
            showPreviewDialog(attachment, frame, "92%", "1100px");
        } else if (mime.startsWith("image/")) {
            Image image = new Image(url, safeFileName(attachment));
            image.setMaxWidth("100%");
            image.setMaxHeight("78vh");
            image.getStyle().set("object-fit", "contain").set("display", "block").set("margin", "auto");
            showPreviewDialog(attachment, image, "85%", "1000px");
        } else if (mime.startsWith("video/") || mime.startsWith("audio/")) {
            IFrame frame = new IFrame(url);
            frame.setWidth("100%");
            frame.setHeight(mime.startsWith("audio/") ? "180px" : "72vh");
            showPreviewDialog(attachment, frame, "85%", "1000px");
        } else {
            UI.getCurrent().getPage().open(url, "_blank");
        }
    }

    private void downloadSelectedAsZip() {
        Set<FileAttachment> selected = new LinkedHashSet<>(grid.getSelectedItems());
        List<UUID> uuids = selected.stream()
            .map(FileAttachment::getFileUuid)
            .filter(java.util.Objects::nonNull)
            .toList();

        if (uuids.isEmpty()) {
            return;
        }

        String query = uuids.stream()
            .map(uuid -> "uploadUuid=" + uuid)
            .collect(java.util.stream.Collectors.joining("&"));
        String url = ApplicationUrlUtil.contextUrl("/api/uploads/download-zip?" + query);
        UI.getCurrent().getPage().open(url, "_blank");
    }

    private void showProperties(FileAttachment attachment) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("File Properties | ព័ត៌មានឯកសារ");
        dialog.setWidth("640px");
        dialog.setMaxWidth("95vw");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("520px", 2));
        form.addFormItem(valueSpan(safeFileName(attachment)), "File name | ឈ្មោះឯកសារ");
        form.addFormItem(valueSpan(attachment.getMimeType()), "MIME type");
        form.addFormItem(valueSpan(formatFileSize(attachment.getFileSize())), "Size | ទំហំ");
        form.addFormItem(valueSpan(String.valueOf(attachment.getFileUuid())), "Upload UUID");
        form.addFormItem(valueSpan(attachment.getChecksumSha256()), "SHA-256");
        form.addFormItem(valueSpan(attachment.getFileCategory() == null ? null : attachment.getFileCategory().name()), "Category");
        form.addFormItem(valueSpan(attachment.getAttachmentType() == null ? null : attachment.getAttachmentType().getAttachmentTypeName()), "Attachment type");
        form.addFormItem(valueSpan(attachment.getDescription()), "Description | ការពិពណ៌នា");
        form.addFormItem(valueSpan(String.valueOf(attachment.getSortOrder())), "Order | លំដាប់");
        form.addFormItem(valueSpan(attachment.getVersion() == null ? "0" : String.valueOf(attachment.getVersion())), "Version");

        Button preview = new Button("Preview", VaadinIcon.EYE.create());
        preview.addClickListener(event -> openPreview(attachment));
        Button download = new Button("Download", VaadinIcon.DOWNLOAD.create());
        download.addClickListener(event -> openDownload(attachment));
        Button close = new Button("Close | បិទ", event -> dialog.close());
        dialog.add(form);
        dialog.getFooter().add(preview, download, close);
        dialog.open();
    }

    private Span valueSpan(String value) {
        Span span = new Span(value == null || value.isBlank() ? "-" : value);
        span.getStyle().set("white-space", "normal").set("word-break", "break-word");
        return span;
    }

    private boolean matchesSearch(FileAttachment item, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return containsIgnoreCase(item.getFileName(), query)
            || containsIgnoreCase(item.getDescription(), query)
            || containsIgnoreCase(item.getMimeType(), query)
            || containsIgnoreCase(item.getChecksumSha256(), query)
            || containsIgnoreCase(item.getFileUuid() == null ? null : item.getFileUuid().toString(), query)
            || containsIgnoreCase(item.getAttachmentType() == null ? null : item.getAttachmentType().getAttachmentTypeName(), query);
    }

    private boolean containsIgnoreCase(String value, String lowerCaseQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }

    private boolean isPdf(FileAttachment attachment) {
        String mime = attachment.getMimeType() == null ? "" : attachment.getMimeType().toLowerCase(Locale.ROOT);
        return "application/pdf".equals(mime) || safeFileName(attachment).toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private boolean isImage(FileAttachment attachment) {
        String mime = attachment.getMimeType() == null ? "" : attachment.getMimeType().toLowerCase(Locale.ROOT);
        return mime.startsWith("image/");
    }

    private void showPreviewDialog(
            FileAttachment attachment,
            Component content,
            String width,
            String maxWidth) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Preview: " + safeFileName(attachment));
        dialog.setWidth(width);
        dialog.setMaxWidth(maxWidth);
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.add(content);
        dialog.getFooter().add(new Button("Close | បិទ", event -> dialog.close()));
        dialog.open();
    }

    private Icon fileIcon(FileAttachment attachment) {
        String mime = attachment.getMimeType() != null
            ? attachment.getMimeType().toLowerCase(Locale.ROOT)
            : "";
        String name = safeFileName(attachment).toLowerCase(Locale.ROOT);

        if ("application/pdf".equals(mime) || name.endsWith(".pdf")) return VaadinIcon.FILE_TEXT_O.create();
        if (mime.startsWith("image/")) return VaadinIcon.PICTURE.create();
        if (mime.startsWith("video/")) return VaadinIcon.FILM.create();
        if (mime.startsWith("audio/")) return VaadinIcon.MUSIC.create();
        if (name.endsWith(".xls") || name.endsWith(".xlsx") || name.endsWith(".csv")) return VaadinIcon.TABLE.create();
        if (name.endsWith(".ppt") || name.endsWith(".pptx")) return VaadinIcon.PRESENTATION.create();
        if (name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z")) return VaadinIcon.ARCHIVES.create();
        if (name.endsWith(".iso")) return VaadinIcon.DISC.create();
        return VaadinIcon.FILE_O.create();
    }

    private void loadAttachmentTypes() {
        attachmentTypes.clear();
        attachmentTypes.addAll(attachmentTypeRepository.findActiveAttachmentTypes());
    }

    private void refreshGrid() {
        String query = searchField.getValue() == null
            ? ""
            : searchField.getValue().trim().toLowerCase(Locale.ROOT);

        List<FileAttachment> visible = attachments.stream()
            .filter(item -> matchesSearch(item, query))
            .toList();

        grid.setItems(new ArrayList<>(visible));
        refreshIndicators();
        updateToolbarState();
    }

    private void refreshIndicators() {

        long totalBytes = attachments.stream()
            .map(FileAttachment::getFileSize)
            .filter(java.util.Objects::nonNull)
            .filter(size -> size > 0)
            .mapToLong(Long::longValue)
            .sum();
        long pdfCount = attachments.stream().filter(this::isPdf).count();
        long imageCount = attachments.stream().filter(this::isImage).count();
        long otherCount = Math.max(0, attachments.size() - pdfCount - imageCount);
        stateBadge.setText(pendingChanges ? "Unsaved changes" : "Saved");
        stateBadge.getElement().getThemeList().remove("success");
        stateBadge.getElement().getThemeList().remove("warning");
        stateBadge.getElement().getThemeList().add(pendingChanges ? "warning" : "success");
    }

    private void markChanged() {
        pendingChanges = true;
        refreshIndicators();
        fireEvent(new FileAttachmentChangedEvent(this, attachments.size(), true));
    }

    private void clearEditorState() {
        editState.clear();
        typeEditors.clear();
        descriptionEditors.clear();
        orderEditors.clear();
    }

    private void styleBadge(Span badge) {
        badge.getElement().getThemeList().add("badge");
        badge.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("white-space", "nowrap");
    }

    private String safeFileName(FileAttachment attachment) {
        return attachment != null && attachment.getFileName() != null
            ? attachment.getFileName()
            : "attachment";
    }

    private String formatFileSize(Long size) {
        if (size == null || size <= 0) return "0 B";
        double value = size;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return unit == 0
            ? String.format(Locale.ROOT, "%.0f %s", value, units[unit])
            : String.format(Locale.ROOT, "%.2f %s", value, units[unit]);
    }
}

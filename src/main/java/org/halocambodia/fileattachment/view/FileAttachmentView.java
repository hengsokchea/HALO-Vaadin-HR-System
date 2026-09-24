package org.halocambodia.fileattachment.view;

import java.util.Locale;

import org.halocambodia.fileattachment.data.FileAttachment;
import org.halocambodia.fileattachment.repository.FileAttachmentRepository;
import org.halocambodia.utility.ApplicationUrlUtil;
import org.halocambodia.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

/**
 * Optional administration/read-only view for the new file_attachment table.
 * It does not replace or modify the existing legacy AttachmentComponent.
 */
@Route(value = "file-attachments", layout = MainLayout.class)
@PageTitle("File Attachments")
@PermitAll
public class FileAttachmentView extends VerticalLayout {

    private final FileAttachmentRepository repository;
    private final Grid<FileAttachment> grid = new Grid<>(FileAttachment.class, false);
    private final TextField search = new TextField();

    public FileAttachmentView(FileAttachmentRepository repository) {
        this.repository = repository;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("File Attachments | ឯកសារភ្ជាប់");
        search.setPlaceholder("Search file, owner type or MIME type...");
        search.setClearButtonVisible(true);
        search.setWidth("360px");
        search.addValueChangeListener(event -> refresh());

        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create(), event -> refresh());
        HorizontalLayout toolbar = new HorizontalLayout(search, refreshButton);
        toolbar.setAlignItems(Alignment.END);

        configureGrid();
        add(title, toolbar, grid);
        expand(grid);
        refresh();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(FileAttachment::getId)
            .setHeader("ID").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(item -> item.getOwnerType() != null ? item.getOwnerType().name() : "")
            .setHeader("Owner Type").setAutoWidth(true);
        grid.addColumn(FileAttachment::getOwnerId)
            .setHeader("Owner ID").setAutoWidth(true);
        grid.addColumn(FileAttachment::getFileName)
            .setHeader("File Name").setFlexGrow(1).setResizable(true);
        grid.addColumn(FileAttachment::getMimeType)
            .setHeader("MIME Type").setAutoWidth(true);
        grid.addColumn(item -> item.getFileCategory() != null ? item.getFileCategory().name() : "")
            .setHeader("Category").setAutoWidth(true);
        grid.addColumn(item -> item.getFileSize() != null ? formatFileSize(item.getFileSize()) : "")
            .setHeader("Size").setAutoWidth(true);
        grid.addColumn(item -> item.getCreatedAt() != null ? item.getCreatedAt().toString() : "")
            .setHeader("Created At").setAutoWidth(true);
        grid.addComponentColumn(item -> {
            Button open = new Button(VaadinIcon.EXTERNAL_LINK.create());
            open.addClickListener(event -> UI.getCurrent().getPage().open(
                ApplicationUrlUtil.contextUrl(
                    "/api/uploads/" + item.getFileUuid() + "/preview"
                ),
                "_blank"
            ));
            open.getElement().setProperty("title", "Open preview");
            return open;
        }).setHeader("Open").setAutoWidth(true).setFlexGrow(0);
    }

    private void refresh() {
        String value = search.getValue() == null
            ? ""
            : search.getValue().trim().toLowerCase(Locale.ROOT);

        grid.setItems(repository.findAll().stream()
            .filter(item -> value.isBlank()
                || contains(item.getFileName(), value)
                || contains(item.getMimeType(), value)
                || (item.getOwnerType() != null
                    && item.getOwnerType().name().toLowerCase(Locale.ROOT).contains(value))
                || (item.getOwnerId() != null
                    && item.getOwnerId().toString().contains(value)))
            .toList());
    }

    private boolean contains(String source, String searchValue) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(searchValue);
    }

    private String formatFileSize(long size) {
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

package org.halocambodia.upload.admin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.operations.DuplicateFileGroup;
import org.halocambodia.upload.operations.DuplicateFileService;
import org.halocambodia.upload.operations.OrphanFileCheckerService;
import org.halocambodia.upload.operations.OrphanFileRecord;
import org.halocambodia.upload.operations.UploadStatisticsService;
import org.halocambodia.upload.operations.UploadSystemSnapshot;
import org.halocambodia.upload.thumbnail.ThumbnailAdminService;
import org.halocambodia.upload.util.FileSizeUtil;
import org.halocambodia.utility.ApplicationUrlUtil;
import org.halocambodia.views.MainLayout;

import java.util.List;

@Route(value ="admin/upload-operations", layout = MainLayout.class)
@PageTitle("Upload Operations")
@PermitAll
@Uses(Icon.class)
public class UploadOperationsView extends VerticalLayout {

    private final UploadStatisticsService statisticsService;
    private final OrphanFileCheckerService orphanService;
    private final DuplicateFileService duplicateService;
    private final ThumbnailAdminService thumbnailAdminService;

    private final HorizontalLayout cards = new HorizontalLayout();
    private final Grid<UploadSession> explorer = new Grid<>(UploadSession.class, false);
    private final Grid<OrphanFileRecord> orphanGrid = new Grid<>(OrphanFileRecord.class, false);
    private final Grid<DuplicateFileGroup> duplicateGrid = new Grid<>(DuplicateFileGroup.class, false);

    public UploadOperationsView(
            UploadStatisticsService statisticsService,
            OrphanFileCheckerService orphanService,
            DuplicateFileService duplicateService,
            ThumbnailAdminService thumbnailAdminService,
            UploadAdminService adminService) {
        this.statisticsService = statisticsService;
        this.orphanService = orphanService;
        this.duplicateService = duplicateService;
        this.thumbnailAdminService = thumbnailAdminService;

        setSizeFull();
        setPadding(true);
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap");

        configureExplorer(adminService);
        configureOrphans();
        configureDuplicates();

        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create(), event -> refreshAll(adminService));
        add(new HorizontalLayout(new H2("Upload Dashboard and Explorer"), refresh), cards,
                new H3("Upload Explorer"), explorer,
                new H3("Orphan File Checker"), orphanGrid,
                new H3("Duplicate Finder"), duplicateGrid);
        expand(explorer);
        refreshAll(adminService);
    }

    private void configureExplorer(UploadAdminService adminService) {
        explorer.setHeight("420px");
        explorer.addColumn(UploadSession::getOriginalFileName).setHeader("File").setAutoWidth(true).setFlexGrow(1);
        explorer.addColumn(UploadSession::getOwnerUsername).setHeader("Owner").setAutoWidth(true);
        explorer.addColumn(item -> item.getStatus() == null ? "" : item.getStatus().name()).setHeader("Status").setAutoWidth(true);
        explorer.addColumn(item -> FileSizeUtil.format(item.getTotalSize() == null ? 0 : item.getTotalSize())).setHeader("Size").setAutoWidth(true);
        explorer.addColumn(UploadSession::getChecksumSha256).setHeader("SHA-256").setAutoWidth(true);
        explorer.addComponentColumn(item -> {
            Button preview = new Button(VaadinIcon.EYE.create(), event -> UI.getCurrent().getPage().open(
                    ApplicationUrlUtil.contextUrl("/api/uploads/" + item.getUploadUuid() + "/preview"), "_blank"));
            preview.setTooltipText("Preview original");
            Button download = new Button(VaadinIcon.DOWNLOAD.create(), event -> UI.getCurrent().getPage().open(
                    ApplicationUrlUtil.contextUrl("/api/uploads/" + item.getUploadUuid() + "/download"), "_blank"));
            download.setTooltipText("Download original");
            Button regenerate = new Button(VaadinIcon.REFRESH.create(), event -> {
                thumbnailAdminService.regenerate(item.getUploadUuid(), 320, 240);
                Notification.show("Thumbnail regenerated");
            });
            regenerate.setTooltipText("Regenerate 320 x 240 thumbnail");
            Button deleteThumbnail = new Button(VaadinIcon.TRASH.create(), event -> {
                boolean deleted = thumbnailAdminService.delete(item.getUploadUuid(), 320, 240);
                Notification.show(deleted ? "Thumbnail deleted" : "Thumbnail did not exist");
            });
            deleteThumbnail.setTooltipText("Delete 320 x 240 thumbnail");
            boolean completed = item.isCompleted();
            preview.setEnabled(completed);
            download.setEnabled(completed);
            regenerate.setEnabled(completed);
            deleteThumbnail.setEnabled(completed);
            return compact(preview, download, regenerate, deleteThumbnail);
        }).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
    }

    private void configureOrphans() {
        orphanGrid.setHeight("280px");
        orphanGrid.addColumn(item -> item.type().name()).setHeader("Problem").setAutoWidth(true);
        orphanGrid.addColumn(OrphanFileRecord::databaseFileName).setHeader("Database file").setAutoWidth(true);
        orphanGrid.addColumn(item -> item.filesystemPath() == null ? "" : item.filesystemPath().toString()).setHeader("Path").setAutoWidth(true).setFlexGrow(1);
        orphanGrid.addColumn(item -> FileSizeUtil.format(item.size())).setHeader("Size").setAutoWidth(true);
        orphanGrid.addColumn(OrphanFileRecord::message).setHeader("Details").setAutoWidth(true);
        orphanGrid.addComponentColumn(item -> {
            Button delete = new Button(VaadinIcon.TRASH.create());
            delete.setEnabled(item.type() == OrphanFileRecord.OrphanType.FILE_WITHOUT_DATABASE_RECORD);
            delete.addClickListener(event -> confirmDeleteOrphan(item));
            return delete;
        }).setHeader("Action").setAutoWidth(true);
    }

    private void configureDuplicates() {
        duplicateGrid.setHeight("280px");
        duplicateGrid.addColumn(DuplicateFileGroup::checksumSha256).setHeader("SHA-256").setAutoWidth(true).setFlexGrow(1);
        duplicateGrid.addColumn(group -> group.uploads().size()).setHeader("Copies").setAutoWidth(true);
        duplicateGrid.addColumn(group -> FileSizeUtil.format(group.fileSize())).setHeader("Each file").setAutoWidth(true);
        duplicateGrid.addColumn(group -> FileSizeUtil.format(group.reclaimableBytes())).setHeader("Potential reclaim").setAutoWidth(true);
        duplicateGrid.setItemDetailsRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(group -> {
            VerticalLayout details = new VerticalLayout();
            details.setPadding(false);
            for (UploadSession upload : group.uploads()) {
                details.add(new Span(upload.getUploadUuid() + " | " + upload.getOriginalFileName()
                        + " | " + upload.getOwnerUsername() + " | " + upload.getCreatedAt()));
            }
            return details;
        }));
    }

    private void refreshAll(UploadAdminService adminService) {
        UploadSystemSnapshot value = statisticsService.snapshot();
        cards.removeAll();
        cards.add(card("Active uploads", value.activeUploads()),
                card("Completed files", value.completedUploads()),
                card("Storage used", FileSizeUtil.format(value.physicalFileBytes())),
                card("Storage free", FileSizeUtil.format(value.usableDiskBytes())),
                card("Thumbnails", value.thumbnailCount()),
                card("Thumbnail cache", FileSizeUtil.format(value.thumbnailBytes())),
                card("Queue", value.thumbnailQueueSize()),
                card("Failed uploads", value.failedUploads()));
        explorer.setItems(adminService.findSessions("", 0, 500).getContent());
        orphanGrid.setItems(orphanService.scan());
        duplicateGrid.setItems(duplicateService.findDuplicates());
    }

    private Component card(String title, Object value) {
        Div card = new Div(new Span(title), new H3(String.valueOf(value)));
        card.getStyle().set("padding", "var(--lumo-space-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("min-width", "180px");
        return card;
    }

    private HorizontalLayout compact(Component... components) {
        HorizontalLayout layout = new HorizontalLayout(components);
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private void confirmDeleteOrphan(OrphanFileRecord item) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete orphan physical file");
        dialog.setText("This permanently deletes: " + item.filesystemPath());
        dialog.setCancelable(true);
        dialog.setConfirmText("Delete");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            orphanService.deletePhysicalOrphan(item);
            orphanGrid.setItems(orphanService.scan());
            Notification.show("Orphan file deleted");
        });
        dialog.open();
    }
}

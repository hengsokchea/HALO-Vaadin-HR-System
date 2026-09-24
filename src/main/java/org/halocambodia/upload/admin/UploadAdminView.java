package org.halocambodia.upload.admin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.halocambodia.upload.audit.UploadAuditEvent;
import org.halocambodia.upload.domain.UploadSession;
import org.halocambodia.upload.util.FileSizeUtil;
import org.halocambodia.views.MainLayout;

import java.time.format.DateTimeFormatter;

@Route(value ="admin/uploads" , layout = MainLayout.class)
@PageTitle("Upload Administration")
@PermitAll
@Uses(Icon.class)
public class UploadAdminView extends VerticalLayout {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final UploadAdminService adminService;
    private final Grid<UploadSession> grid = new Grid<>(UploadSession.class, false);
    private final TextField search = new TextField();
    private final CallbackDataProvider<UploadSession, Void> dataProvider;
    private final Span summary = new Span();

    public UploadAdminView(UploadAdminService adminService) {
        this.adminService = adminService;
        this.dataProvider = new CallbackDataProvider<>(
                query -> adminService.findSessions(
                                search.getValue(),
                                query.getOffset() / Math.max(query.getLimit(), 1),
                                query.getLimit())
                        .getContent().stream(),
                query -> Math.toIntExact(adminService.findSessions(search.getValue(), 0, 1).getTotalElements())
        );

        setSizeFull();
        setPadding(true);
        configureToolbar();
        configureGrid();
        refreshSummary();
        add(new H2("Upload Administration"), summary, buildToolbar(), grid);
        expand(grid);
    }

    private void configureToolbar() {
        search.setPlaceholder("Search owner or file name");
        search.setClearButtonVisible(true);
        search.setWidth("340px");
        search.addValueChangeListener(event -> refreshAll());
    }

    private HorizontalLayout buildToolbar() {
        Button refresh = new Button("Refresh", VaadinIcon.REFRESH.create(),
                event -> dataProvider.refreshAll());
        HorizontalLayout toolbar = new HorizontalLayout(search, refresh);
        toolbar.setAlignItems(Alignment.END);
        return toolbar;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setPageSize(50);
        grid.setItems(dataProvider);

        grid.addColumn(UploadSession::getOriginalFileName)
                .setHeader("File name").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(UploadSession::getOwnerUsername)
                .setHeader("Owner").setAutoWidth(true);
        grid.addColumn(session -> session.getUploadType() == null ? "" : session.getUploadType().name())
                .setHeader("Type").setAutoWidth(true);
        grid.addColumn(session -> session.getStatus() == null ? "" : session.getStatus().name())
                .setHeader("Status").setAutoWidth(true);
        grid.addColumn(session -> FileSizeUtil.format(session.getTotalSize() == null ? 0 : session.getTotalSize()))
                .setHeader("Total size").setAutoWidth(true);
        grid.addColumn(session -> String.format("%.1f%%", session.getProgressPercent()))
                .setHeader("Progress").setAutoWidth(true);
        grid.addColumn(session -> session.getCreatedAt() == null ? "" : DATE_TIME.format(session.getCreatedAt()))
                .setHeader("Created").setAutoWidth(true);
        grid.addColumn(session -> session.getUpdatedAt() == null ? "" : DATE_TIME.format(session.getUpdatedAt()))
                .setHeader("Updated").setAutoWidth(true);
        grid.addComponentColumn(session -> {
            Button audit = new Button(VaadinIcon.SEARCH.create(), event -> openAuditDialog(session));
            audit.setTooltipText("Audit history");
            Button reactivate = new Button(VaadinIcon.ROTATE_LEFT.create(), event -> reactivate(session));
            reactivate.setTooltipText("Reactivate failed, cancelled, expired, or paused upload");
            reactivate.setEnabled(session.getStatus() != null && switch (session.getStatus()) {
                case FAILED, CANCELLED, EXPIRED, PAUSED -> true;
                default -> false;
            });
            Button cancel = new Button(VaadinIcon.BAN.create(), event -> confirmCancel(session));
            cancel.setTooltipText("Cancel upload and remove temporary chunks");
            cancel.setEnabled(session.getStatus() != null && switch (session.getStatus()) {
                case PENDING, UPLOADING, PAUSED, FAILED, EXPIRED -> true;
                default -> false;
            });
            return new HorizontalLayout(audit, reactivate, cancel);
        }).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
    }

    private void refreshAll() {
        dataProvider.refreshAll();
        refreshSummary();
    }

    private void refreshSummary() {
        UploadAdminSummary value = adminService.summary();
        summary.setText(String.format(
                "Total: %d | Active: %d | Completed: %d | Failed: %d | Cancelled: %d | Expired: %d",
                value.total(), value.active(), value.completed(), value.failed(), value.cancelled(), value.expired()));
    }

    private void confirmCancel(UploadSession session) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Cancel upload");
        dialog.setText("Cancel " + session.getOriginalFileName()
                + " and delete its temporary chunks? Completed files are never deleted here.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Cancel upload");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> {
            try {
                adminService.cancel(session.getUploadUuid(), true);
                Notification.show("Upload cancelled");
                refreshAll();
            } catch (RuntimeException exception) {
                Notification.show(exception.getMessage(), 5000, Notification.Position.MIDDLE);
            }
        });
        dialog.open();
    }

    private void reactivate(UploadSession session) {
        try {
            adminService.reactivate(session.getUploadUuid());
            Notification.show("Upload reactivated. The owner can resume from the first missing chunk.");
            refreshAll();
        } catch (RuntimeException exception) {
            Notification.show(exception.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void openAuditDialog(UploadSession session) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Upload audit: " + session.getOriginalFileName());
        dialog.setWidth("min(1000px, 95vw)");
        dialog.setHeight("min(700px, 90vh)");

        Grid<UploadAuditEvent> auditGrid = new Grid<>(UploadAuditEvent.class, false);
        auditGrid.setSizeFull();
        auditGrid.setItems(adminService.findAuditEvents(session.getUploadUuid()));
        auditGrid.addColumn(event -> event.getCreatedAt() == null ? "" : DATE_TIME.format(event.getCreatedAt()))
                .setHeader("Time").setAutoWidth(true);
        auditGrid.addColumn(event -> event.getAction() == null ? "" : event.getAction().name())
                .setHeader("Action").setAutoWidth(true);
        auditGrid.addColumn(UploadAuditEvent::getUsername).setHeader("User").setAutoWidth(true);
        auditGrid.addColumn(event -> FileSizeUtil.format(event.getBytesProcessed() == null ? 0 : event.getBytesProcessed()))
                .setHeader("Bytes").setAutoWidth(true);
        auditGrid.addColumn(event -> event.isSuccess() ? "Yes" : "No")
                .setHeader("Success").setAutoWidth(true);
        auditGrid.addColumn(UploadAuditEvent::getMessage)
                .setHeader("Message").setFlexGrow(1);

        Span uploadId = new Span("Upload ID: " + session.getUploadUuid());
        VerticalLayout content = new VerticalLayout(uploadId, auditGrid);
        content.setSizeFull();
        content.expand(auditGrid);
        dialog.add(content);
        dialog.getFooter().add(new Button("Close", event -> dialog.close()));
        dialog.open();
    }
}

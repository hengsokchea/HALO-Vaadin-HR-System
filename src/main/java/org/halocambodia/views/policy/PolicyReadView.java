package org.halocambodia.views.policy;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.halocambodia.data.*;
import org.halocambodia.fileattachment.component.FilePreviewDialog;
import org.halocambodia.services.*;
import org.halocambodia.utility.ApplicationUrlUtil;
import org.halocambodia.utility.UrlUtils;
import org.halocambodia.views.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;


@Route(value = "policy-read", layout = MainLayout.class)
@PageTitle("Read Policy")
@PermitAll
public class PolicyReadView extends VerticalLayout implements HasUrlParameter<String> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PolicyReadView.class);
    //private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
   /// private static final DateTimeFormatter DATE_TIME_FORMAT =  DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private final PolicyReadService readService;
    private final PolicyAcknowledgeService acknowledgeService;
    private final PolicyAccessLogService accessLogService;

    private PolicyReadDto currentPolicy;
    private String urlRead;
    
    @Autowired
    private QRCodeService qrCodeService;

    public PolicyReadView( PolicyReadService readService, PolicyAcknowledgeService acknowledgeService, PolicyAccessLogService accessLogService) {

        this.readService = readService;
        this.acknowledgeService = acknowledgeService;
        this.accessLogService = accessLogService;

        configureLayout();
    }

    private void configureLayout() {
        setWidthFull();
        setMaxWidth("1100px");
        setMargin(true);
        setPadding(true);
        setSpacing(true);

        getStyle()
                .set("margin-left", "auto")
                .set("margin-right", "auto");
    }

    /**
     * Receives the public token from:
     *
     * /policy/read/{publicToken}
     */
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        removeAll();
        currentPolicy = null;

        if (parameter == null || parameter.isBlank()) {
            renderUnavailable("The policy link is missing.");
            return;
        }

        final UUID publicToken;

        try {
            publicToken = UUID.fromString(parameter.trim());  
        } catch (IllegalArgumentException exception) {
            renderUnavailable("The policy link is invalid.");
            return;
        }

        loadAndRenderPolicy(publicToken);
    }

    private void loadAndRenderPolicy(UUID publicToken) {
        try {
            currentPolicy = readService
                    .findAvailableByPublicToken(publicToken)
                    .orElse(null);

            if (currentPolicy == null) {
                renderUnavailable(
                        "The policy does not exist, is not published, "
                                + "is inactive, has expired, or is not yet effective."
                );
                return;
            }

            // The policy loaded successfully.
            renderPolicy(currentPolicy);
            this.urlRead=buildPolicyReadUrl(publicToken);

        } catch (Exception exception) {
            log.error("Unable to load policy for token {}", publicToken, exception);

            removeAll();
            renderUnavailable(
                    "The policy could not be loaded. Please try again later."
            );
            return;
        }

        /*
         * Access logging must not prevent the policy from being displayed.
         */
        recordPolicyViewSafely(publicToken);
    }
    private void recordPolicyViewSafely(UUID publicToken) {
        try {
            readService.findAvailableEntity(publicToken)
                    .ifPresent(policy -> {
                        try {
                            accessLogService.record(
                                    policy,
                                    PolicyAccessAction.VIEWED,
                                    null,
                                    currentRequest()
                            );
                        } catch (Exception exception) {
                            log.warn(
                                    "Unable to record policy view for token {}",
                                    publicToken,
                                    exception
                            );
                        }
                    });
        } catch (Exception exception) {
            log.warn(
                    "Unable to load policy entity for access logging. Token: {}",
                    publicToken,
                    exception
            );
        }
    }

    private void renderPolicy(PolicyReadDto policy) {
        add(
                buildHeader(policy),
                buildActions(policy),
                buildMetadata(policy)
        );

        if (hasText(policy.summary())) {
            add(buildTextSection("Summary", policy.summary()));
        }

        if (hasText(policy.content())) {
            add(buildTextSection("Policy Content", policy.content()));
        }

        add(buildAttachmentsSection(policy));
        add(buildAcknowledgementSection(policy));
        add(buildFooter(policy));
    }

    private Component buildHeader(PolicyReadDto policy) {
        VerticalLayout header = new VerticalLayout();
        header.setPadding(false);
        header.setSpacing(false);
        header.setWidthFull();

        if (hasText(policy.titleKh())) {
            H2 titleKh = new H2(policy.titleKh());
            titleKh.getStyle()
                    .set("margin", "0")
                    .set("font-weight", "600")
                    .set("color", "var(--lumo-secondary-text-color)");

            header.add(titleKh);
        }

        H1 titleEn = new H1(
                hasText(policy.titleEn())
                        ? policy.titleEn()
                        : "Policy"
        );

        titleEn.getStyle()
                .set("margin-top", "4px")
                .set("margin-bottom", "0");

        header.add(titleEn);

        if (hasText(policy.code())) {
            Span code = new Span("Policy Code: " + policy.code());
            code.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");

            header.add(code);
        }

        return header;
    }

    private Component buildActions(PolicyReadDto policy) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setWrap(true);
        actions.setAlignItems(Alignment.CENTER);


        Button qrButton = new Button( "QR Code", VaadinIcon.QRCODE.create(), event -> showQrCodeDialog(policy));

        Button backButton = new Button(
                "Back",
                VaadinIcon.ARROW_LEFT.create(),
                event -> event.getSource()
                        .getUI()
                        .ifPresent(ui ->
                                ui.navigate(PolicyAcknowledgeView.class))
        );


        qrButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        actions.add(backButton, spacer,  qrButton);

        return actions;
    }

    private Component buildMetadata(PolicyReadDto policy) {
        HorizontalLayout metadata = new HorizontalLayout();
        metadata.setWidthFull();
        metadata.setPadding(false);
        metadata.setSpacing(false);
        metadata.setWrap(true);

        String category = combineBilingual(
                policy.categoryNameEn(),
                policy.categoryNameKh()
        );

        metadata.add(
                meta("Code", safeValue(policy.code())),
                meta("Category", category),
                meta("Version", formatVersion(policy.version())),
                meta("Effective Date", policy.effectiveDate() == null ? "—" :DateTimeUtilFormart.DATE_FORMATTER.format(  policy.effectiveDate())
                )
        );

        metadata.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        return metadata;
    }

    private Component buildTextSection(String title, String text) {
        VerticalLayout card = createCard();

        H3 heading = new H3(title);
        heading.getStyle().set("margin", "0");

        Paragraph content = new Paragraph(text);
        content.getStyle()
                .set("white-space", "pre-wrap")
                .set("line-height", "1.65")
                .set("margin-bottom", "0");

        card.add(heading, content);

        return card;
    }

    private Component buildAttachmentsSection(PolicyReadDto policy) {
        VerticalLayout card = createCard();

        int attachmentCount = policy.attachments() == null
                ? 0
                : policy.attachments().size();

        H3 heading = new H3(
                "Attachments (" + attachmentCount + ")"
        );
        heading.getStyle().set("margin", "0");

        card.add(heading);

        if (attachmentCount == 0) {
            Span emptyMessage = new Span("No attachments.");
            emptyMessage.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");

            card.add(emptyMessage);
            return card;
        }

        for (PolicyAttachmentDto file : policy.attachments()) {
            card.add(buildAttachmentRow(policy, file));
        }

        return card;
    }

    private Component buildAttachmentRow(
            PolicyReadDto policy,
            PolicyAttachmentDto file) {

        VerticalLayout wrapper = new VerticalLayout();
        wrapper.setWidthFull();
        wrapper.setPadding(false);
        wrapper.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        row.setAlignItems(Alignment.CENTER);

        Icon fileIcon = getFileIcon(file.fileName());
        fileIcon.setSize("20px");
        fileIcon.getStyle().set("flex-shrink", "0");

        VerticalLayout fileInformation = new VerticalLayout();
        fileInformation.setPadding(false);
        fileInformation.setSpacing(false);

        Span fileName = new Span(
                hasText(file.fileName())
                        ? file.fileName()
                        : "Unnamed attachment"
        );

        fileName.getStyle()
                .set("font-weight", "600")
                .set("overflow-wrap", "anywhere");

        fileInformation.add(fileName);

        if (file.fileSize() != null) {
            Span fileSize = new Span(formatFileSize(file.fileSize()));
            fileSize.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)");

            fileInformation.add(fileSize);
        }

        Div spacer = new Div();
        spacer.getStyle().set("flex-grow", "1");

        
        String url = ApplicationUrlUtil.contextUrl(
                "/api/uploads/" + file.fileUuid()
            );

        if (file.previewable()) {
            Button previewButton = new Button("Preview | មើល", VaadinIcon.EYE.create(), event -> FilePreviewDialog.openPreview(file.fileName(),file.mimeType(), url + "/preview") );

            previewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE );

            previewButton.getElement().setAttribute(
                    "title",
                    "Preview " + safeValue(file.fileName())
            );

            row.add(fileIcon, fileInformation, spacer, previewButton);
        } else {
            row.add(fileIcon, fileInformation, spacer);
        }

        Anchor downloadLink = createActionLink(
        		url + "/download",
                "Download | ទាញយក",
                VaadinIcon.DOWNLOAD.create()
        );

        downloadLink.getElement()
                .setAttribute("download", true);

        row.add(downloadLink);
        wrapper.add(row);

        if (hasText(file.description())) {
            Span description = new Span(file.description());
            description.getStyle()
                    .set("margin-left", "28px")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("white-space", "pre-wrap");

            wrapper.add(description);
        }

        wrapper.getStyle()
                .set("padding", "12px 0")
                .set(
                        "border-bottom",
                        "1px solid var(--lumo-contrast-10pct)"
                );

        return wrapper;
    }

    private Component buildAcknowledgementSection(
            PolicyReadDto policy) {

        VerticalLayout card = createCard();

        H3 heading = new H3("Policy Acknowledgement");
        heading.getStyle().set("margin", "0");

        card.add(heading);

        if (!policy.requiredAcknowledge()) {
            Span notRequired = new Span(
                    "Acknowledgement is not required for this policy."
            );

            notRequired.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");

            card.add(notRequired);
            return card;
        }

        if (policy.acknowledgedCurrentVersion()) {
            Span acknowledged = new Span( policy.acknowledgedAt() == null ? "✓ Policy acknowledged" : "✓ Acknowledged on "  + DateTimeUtilFormart.DATE_TIME_FORMATTER.format(  policy.acknowledgedAt())  );

            acknowledged.getStyle()
                    .set("font-weight", "700")
                    .set("color", "var(--lumo-success-text-color)");

            card.add(acknowledged);
            return card;
        }

        Span pendingStatus = new Span("Pending acknowledgement");
        pendingStatus.getElement().getThemeList().add("badge warning");

        Checkbox confirmation = new Checkbox(
                "I confirm that I have read and understood this policy."
        );
        confirmation.setWidthFull();

        Button acknowledgeButton = new Button(
                "Acknowledge Policy",
                VaadinIcon.CHECK.create(),
                event -> acknowledgePolicy(
                        policy,
                        event.getSource()
                )
        );

        acknowledgeButton.setEnabled(false);
        acknowledgeButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_SUCCESS
        );

        confirmation.addValueChangeListener(event ->
                acknowledgeButton.setEnabled(Boolean.TRUE.equals(event.getValue()))
        );

        card.add(pendingStatus, confirmation, acknowledgeButton);

        return card;
    }

    private Component buildFooter(PolicyReadDto policy) {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setPadding(true);
        footer.setSpacing(true);
        footer.setWrap(true);
        footer.setAlignItems(Alignment.CENTER);

        Span version = new Span("Version: " + formatVersion(policy.version()));

        Span effectiveDate = new Span("Effective: " + (policy.effectiveDate() == null ? "—"  : DateTimeUtilFormart.DATE_FORMATTER.format(  policy.effectiveDate())));

        version.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        effectiveDate.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        footer.add(version, effectiveDate);

        footer.getStyle()
                .set(
                        "border-top",
                        "1px solid var(--lumo-contrast-10pct)"
                )
                .set("margin-top", "10px");

        return footer;
    }



    private void acknowledgePolicy(
            PolicyReadDto policy,
            Button acknowledgeButton) {

        acknowledgeButton.setEnabled(false);

        try {
            Policy entity = readService
                    .findAvailableEntity(policy.publicToken())
                    .orElse(null);

            if (entity == null) {
                showErrorNotification(
                        "The policy is no longer available."
                );
                acknowledgeButton.setEnabled(true);
                return;
            }

            acknowledgeService.acknowledge(
                    entity,
                    currentRequest()
            );

            Notification notification = Notification.show(
                    "Policy acknowledged successfully.",
                    2500,
                    Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(
                    NotificationVariant.LUMO_SUCCESS
            );

            /*
             * Refresh the Vaadin view without a browser reload. This avoids
             * duplicate route requests and duplicate access-log operations.
             */
            Optional<PolicyReadDto> refreshedPolicy = readService
                    .findAvailableByPublicToken(policy.publicToken());

            if (refreshedPolicy.isPresent()) {
                currentPolicy = refreshedPolicy.get();
                removeAll();
                renderPolicy(currentPolicy);
            } else {
                /*
                 * The acknowledgement was saved. Keep the current policy
                 * visible even if a later refresh query unexpectedly fails.
                 */
                removeAll();
                renderPolicy(policy);
            }

        } catch (Exception exception) {
            acknowledgeButton.setEnabled(true);

            log.error(
                    "Unable to acknowledge policy with token {}",
                    policy.publicToken(),
                    exception
            );

            showErrorNotification(
                    "The acknowledgement could not be saved: "
                            + getSafeErrorMessage(exception)
            );
        }
    }

    private String getSafeErrorMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error";
        }

        Throwable cause = exception;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }

        return message;
    }

    private Component meta(String label, String value) {
        Span title = new Span(label);
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span text = new Span(
                hasText(value) ? value : "—"
        );

        text.getStyle()
                .set("font-weight", "600")
                .set("overflow-wrap", "anywhere");

        Div box = new Div(title, text);

        box.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("flex", "1 1 180px")
                .set("min-width", "160px")
                .set("padding", "12px");

        return box;
    }

    private VerticalLayout createCard() {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);

        card.getStyle()
                .set(
                        "border",
                        "1px solid var(--lumo-contrast-10pct)"
                )
                .set("border-radius", "12px")
                .set("background", "var(--lumo-base-color)")
                .set(
                        "box-shadow",
                        "0 1px 2px var(--lumo-contrast-10pct)"
                );

        return card;
    }

    private Anchor createActionLink(
            String href,
            String label,
            Icon icon) {

        icon.setSize("16px");

        Span text = new Span(label);

        HorizontalLayout content = new HorizontalLayout(
                icon,
                text
        );

        content.setPadding(false);
        content.setSpacing(true);
        content.setAlignItems(Alignment.CENTER);

        Anchor anchor = new Anchor(href, content);

        anchor.getStyle()
                .set("text-decoration", "none")
                .set("font-weight", "600")
                .set("white-space", "nowrap");

        return anchor;
    }

    private Icon getFileIcon(String fileName) {
        if (!hasText(fileName)) {
            return VaadinIcon.FILE.create();
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return VaadinIcon.FILE_TEXT.create();
        }

        if (lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".png")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".webp")) {

            return VaadinIcon.PICTURE.create();
        }

        if (lowerName.endsWith(".doc")
                || lowerName.endsWith(".docx")) {

            return VaadinIcon.FILE_TEXT.create();
        }

        if (lowerName.endsWith(".xls")
                || lowerName.endsWith(".xlsx")
                || lowerName.endsWith(".csv")) {

            return VaadinIcon.TABLE.create();
        }

        if (lowerName.endsWith(".zip")
                || lowerName.endsWith(".rar")
                || lowerName.endsWith(".7z")) {

            return VaadinIcon.ARCHIVES.create();
        }

        return VaadinIcon.FILE.create();
    }

    private void renderUnavailable(String message) {
        VerticalLayout unavailableCard = createCard();
        unavailableCard.setAlignItems(Alignment.CENTER);

        Icon warningIcon = VaadinIcon.WARNING.create();
        warningIcon.setSize("42px");
        warningIcon.getStyle()
                .set("color", "var(--lumo-error-text-color)");

        H2 heading = new H2("Policy unavailable");
        heading.getStyle().set("margin-bottom", "0");

        Paragraph description = new Paragraph(message);
        description.getStyle()
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        Button backButton = new Button(
                "Back to Policies",
                VaadinIcon.ARROW_LEFT.create(),
                event -> event.getSource()
                        .getUI()
                        .ifPresent(ui ->
                                ui.navigate(
                                        PolicyAcknowledgeView.class
                                )
                        )
        );

        unavailableCard.add(
                warningIcon,
                heading,
                description,
                backButton
        );

        add(unavailableCard);
     
    }

    private void showErrorNotification(String message) {
        Notification notification = Notification.show(message);
        notification.addThemeVariants(
                NotificationVariant.LUMO_ERROR
        );
    }

    private HttpServletRequest currentRequest() {
        VaadinServletRequest request =
                VaadinServletRequest.getCurrent();

        return request == null
                ? null
                : request.getHttpServletRequest();
    }

    private String combineBilingual(
            String english,
            String khmer) {

        boolean hasEnglish = hasText(english);
        boolean hasKhmer = hasText(khmer);

        if (hasEnglish && hasKhmer) {
            return english + " | " + khmer;
        }

        if (hasEnglish) {
            return english;
        }

        if (hasKhmer) {
            return khmer;
        }

        return "—";
    }

    private String safeValue(String value) {
        return hasText(value) ? value : "—";
    }

    private String formatVersion(Object version) {
        return version == null ? "—" : String.valueOf(version);
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "—";
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        double kilobytes = bytes / 1024.0;

        if (kilobytes < 1024) {
            return String.format("%.1f KB", kilobytes);
        }

        double megabytes = kilobytes / 1024.0;

        if (megabytes < 1024) {
            return String.format("%.1f MB", megabytes);
        }

        double gigabytes = megabytes / 1024.0;

        return String.format("%.2f GB", gigabytes);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
    
    private void showQrCodeDialog(PolicyReadDto policy) {

        if (!validatePolicyForSharing(policy)) {
            return;
        }

       // String publicUrl = buildPublicPolicyUrl(policy);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(
                "Share Policy | ចែករំលែកគោលនយោបាយ");
        dialog.setWidth("460px");
        dialog.setMaxWidth("95vw");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setWidthFull();
        contentLayout.setPadding(false);
        contentLayout.setSpacing(true);
        contentLayout.setAlignItems(Alignment.CENTER);

        String title = policy.titleEn();

        if (title == null || title.isBlank()) {
            title = policy.code();
        }

        H3 policyTitle = new H3(title);
        policyTitle.getStyle()
                .set("margin", "0")
                .set("text-align", "center")
                .set("overflow-wrap", "anywhere");

        Paragraph khmerTitle = new Paragraph(
                policy.titleKh() == null
                        ? ""
                        : policy.titleKh());

        khmerTitle.setVisible( policy.titleKh() != null ?false : !policy.titleKh().isBlank());

        khmerTitle.getStyle()
                .set("margin", "0")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        Image qrImage = new Image( qrCodeService.createStreamResource(this.urlRead), "QR Code for " + title);

        qrImage.setWidth("300px");
        qrImage.setHeight("300px");

        qrImage.getStyle()
                .set("max-width", "80vw")
                .set("object-fit", "contain")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.5rem")
                .set("background", "white");

        Paragraph link = new Paragraph(this.urlRead);

        link.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("overflow-wrap", "anywhere")
                .set("text-align", "center")
                .set("margin", "0");

        Button copyButton = new Button("Copy Link | ចម្លងតំណ", event -> copyPublicLink(policy));

        copyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        contentLayout.add(
                policyTitle,
                khmerTitle,
                qrImage,
                link,
                copyButton);

        dialog.add(contentLayout);

        Button closeButton = new Button(
                "Close | បិទ",
                event -> dialog.close());

        dialog.getFooter().add(closeButton);
        dialog.open();
    }

    private boolean validatePolicyForSharing(PolicyReadDto policy) {
        if (policy == null ) {
            showShareWarning("Please save the policy before sharing | សូមរក្សាទុកគោលនយោបាយមុនពេលចែករំលែក");
            return false;
        }

        if (policy.publicToken() == null) {
            showShareWarning("Public token is missing. Save the policy again | មិនមានលេខសម្គាល់សាធារណៈទេ");
            return false;
        }

        return true;
    }

    private void showShareWarning(String message) {
        Notification notification = Notification.show( message, 4000, Notification.Position.MIDDLE );
        notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
    }
    
    private void copyPublicLink(PolicyReadDto policy) {
        if (!validatePolicyForSharing(policy)) {
            return;
        }

      

        UI.getCurrent().getPage().executeJs(
            """
            const value = $0;
            if (navigator.clipboard && window.isSecureContext) {
                return navigator.clipboard.writeText(value);
            }
            const textArea = document.createElement('textarea');
            textArea.value = value;
            textArea.style.position = 'fixed';
            textArea.style.opacity = '0';
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
            """,
            this.urlRead
        );

        Notification.show(
            "Public link copied | បានចម្លងតំណសាធារណៈ",
            2500,
            Notification.Position.MIDDLE
        );
    }
    
    private String buildPolicyReadUrl(UUID publicToken) {

        if (publicToken == null) {
            throw new IllegalArgumentException(
                    "Policy public token is required."
            );
        }

        String route = RouteConfiguration
                .forSessionScope()
                .getUrl(
                        PolicyReadView.class,
                        publicToken.toString()
                );

        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/")
                .path(route)
                .build()
                .toUriString();
    }
}
package org.halocambodia.views.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.data.Policy;
import org.halocambodia.data.PolicyCategory;
import org.halocambodia.data.PolicyCategoryRepository;

import org.halocambodia.fileattachment.component.FileAttachmentComponent;
import org.halocambodia.fileattachment.data.FileAttachmentOwnerType;
import org.halocambodia.fileattachment.service.FileAttachmentService;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PolicyService;
import org.halocambodia.services.QRCodeService;
import org.halocambodia.services.UserService;
import org.halocambodia.upload.domain.UploadType;
import org.halocambodia.utility.UrlUtils;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.component.UI;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

@Route(value = "policies", layout = MainLayout.class)
@PageTitle("Policies")
@PermitAll
@Uses(Icon.class)
public class PolicyView  extends PageDialogLayout<Policy, PolicyService> {

    // ---------------------------------------------------------------------
    // Policy fields
    // ---------------------------------------------------------------------

    private final TextField code =new TextField("Code | កូដ");
    private final TextField titleEn = new TextField("Title (English) | ចំណងជើង (អង់គ្លេស)"  );
    private final TextField titleKh = new TextField("Title (Khmer) | ចំណងជើង (ខ្មែរ)");
    private final TextArea summary = new TextArea("Summary | សេចក្តីសង្ខេប" );
    private final TextArea content =new TextArea("Content | ខ្លឹមសារ");
    private final DatePicker effectiveDate =new DatePicker("Effective Date | ថ្ងៃចាប់ផ្តើមអនុវត្ត");
    private final DatePicker expiryDate =new DatePicker("Expiry Date | ថ្ងៃផុតកំណត់");
    private final Checkbox published =new Checkbox("Published | បានចុះផ្សាយ");
    private final Checkbox requiredAcknowledge =new Checkbox("Require Acknowledge | ត្រូវការបញ្ជាក់");
    private final Checkbox active =new Checkbox("Active | សកម្ម");
    private final Select<PolicyCategory> policyCategory =new Select<>();
    
    private Long originalPolicyCategoryId;
    private String originalPolicyCode;
    private final List<PolicyCategory> availablePolicyCategories =
            new ArrayList<>();

    private final PolicyCategoryRepository policyCategoryRepository;

    private final FileAttachmentComponent fileAttachmentComponent;


    private GridContextMenu<Policy> policyContextMenu;

    private Tabs tabs;
    private VerticalLayout detailTabContent;
    private VerticalLayout attachmentTabContent;
    private FormLayout basicForm;
    
    private final String publicBaseUrl;
    
    @Autowired
    private QRCodeService qrCodeService;

    public PolicyView( PolicyService service, PolicyCategoryRepository policyCategoryRepository, AttachmentTypeRepository attachmentTypeRepository,FileAttachmentService fileAttachmentService, UserService userService, AuthenticatedUser authenticatedUser) {

        super( Policy.class, service, userService, authenticatedUser );

        this.policyCategoryRepository =policyCategoryRepository;

        this.fileAttachmentComponent =new FileAttachmentComponent( fileAttachmentService, attachmentTypeRepository, FileAttachmentOwnerType.POLICY, UploadType.POLICY, "");
        this.fileAttachmentComponent.setAttachmentTypeRequired(true);
        this.fileAttachmentComponent.addChangedListener(event -> {
  
       });
        
        this.publicBaseUrl = UrlUtils.getBaseUrl();

    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        this.enableToggleColumn = false;
        this.toggleColumnFrozen = false;

        configureGrid();
        configurePolicyContextMenu();
        configureEditorLayout();
        binderField();
        loadPolicyCategories();
    }

    // ---------------------------------------------------------------------
    // Editor layout
    // ---------------------------------------------------------------------

    @Override
    protected void configureEditorLayout()
            throws Exception {

        java.util.function.Function<
                Component[],
                FormLayout
            > form = components -> {

                FormLayout layout =
                        new FormLayout(components);

                layout.setWidthFull();

                layout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep(
                        "0",
                        1
                    ),
                    new FormLayout.ResponsiveStep(
                        "600px",
                        2
                    ),
                    new FormLayout.ResponsiveStep(
                        "1000px",
                        3
                    ),
                    new FormLayout.ResponsiveStep(
                        "1300px",
                        4
                    )
                );

                for (Component component : components) {
                    if (component
                            instanceof
                            com.vaadin.flow.component.HasSize hasSize) {

                        hasSize.setWidthFull();
                    }
                }

                return layout;
            };

        configurePolicyFields();

        basicForm = form.apply(
            new Component[] {
                code,
                titleEn,
                titleKh,
                policyCategory,
                effectiveDate,
                expiryDate,
                published,
                requiredAcknowledge,
                active,
                summary,
                content
            }
        );

        basicForm.setColspan(summary, 4);
        basicForm.setColspan(content, 4);

        tabs = new Tabs();

        Tab detailTab =new Tab("Policy Detail | ព័ត៌មានគោលនយោបាយ" );

        Tab attachmentTab =new Tab( "Attachments | ឯកសារភ្ជាប់");

        tabs.add(
            detailTab,
            attachmentTab
        );

        detailTabContent = new VerticalLayout();

        detailTabContent.setPadding(false);
        detailTabContent.setSpacing(false);
        detailTabContent.setWidthFull();
        detailTabContent.add(basicForm);

        attachmentTabContent =new VerticalLayout();

        attachmentTabContent.setPadding(false);
        attachmentTabContent.setSpacing(false);
        attachmentTabContent.setWidthFull();


        attachmentTabContent.add(fileAttachmentComponent);

        Div contentContainer =new Div(detailTabContent,attachmentTabContent);

        contentContainer.setWidthFull();
        contentContainer.getStyle()
            .set("padding", "1rem 0")
            .set("box-sizing", "border-box");

        tabs.addSelectedChangeListener(event -> {
            boolean showingDetails =event.getSelectedTab() == detailTab;
            detailTabContent.setVisible(showingDetails);
            attachmentTabContent.setVisible(!showingDetails);
        });

        detailTabContent.setVisible(true);
        attachmentTabContent.setVisible(false);

        Div wrapper =
                new Div(
                    tabs,
                    contentContainer
                );

        wrapper.setWidthFull();
        wrapper.getStyle()
            .set("padding", "0 1rem")
            .set("box-sizing", "border-box");

        editorLayout.add(wrapper);

        configureEditorFooter();
    }

    private void configurePolicyFields() {
        code.setReadOnly(true);
        code.setRequiredIndicatorVisible(false);
        code.setHelperText(
            "Generated automatically, for example HR001"
        );

        /*
         * Category can be changed on both New and Edit.
         */
        policyCategory.setReadOnly(false);

        policyCategory.addValueChangeListener(event -> {
            /*
             * Ignore changes caused by binder.readBean().
             */
            if (!event.isFromClient()) {
                return;
            }

            PolicyCategory selectedCategory = event.getValue();

            if (selectedCategory == null) {
                code.clear();
                return;
            }

            /*
             * If editing and the user selects the original category
             * again, restore the original policy code.
             */
            if (entity != null
                    && entity.getId() != null
                    && Objects.equals(
                        selectedCategory.getId(),
                        originalPolicyCategoryId
                    )) {

                code.setValue(
                    originalPolicyCode != null
                        ? originalPolicyCode
                        : ""
                );

                return;
            }

            try {
                /*
                 * Show a preview. The service generates the final
                 * concurrency-safe code during save.
                 */
                String nextCode =
                        service.previewNextCode(
                            selectedCategory
                        );

                code.setValue(nextCode);

            } catch (Exception exception) {
                code.clear();

                Notification.show(
                    exception.getMessage(),
                    4000,
                    Notification.Position.MIDDLE
                );
            }
        });

        titleEn.setRequired(true);
        titleEn.setRequiredIndicatorVisible(true);

        titleKh.setRequired(true);
        titleKh.setRequiredIndicatorVisible(true);

        policyCategory.setRequiredIndicatorVisible(true);

        effectiveDate.setRequired(true);
        effectiveDate.setRequiredIndicatorVisible(true);

        summary.setHeight("80px");
        summary.setMaxLength(4000);

        content.setHeight("150px");

        expiryDate.setPlaceholder(
            "Leave empty if no expiry | "
                + "ទុកចោលប្រសិនបើគ្មានកាលបរិច្ឆេទផុតកំណត់"
        );
    }

    private boolean isCreatingPolicy() {
        return entity == null
                || entity.getId() == null;
    }

    private void loadPolicyCategories() {
        availablePolicyCategories.clear();
        availablePolicyCategories.addAll(
            policyCategoryRepository
                .findByObsoleteDateIsNullOrderByNameEnAsc()
        );

        policyCategory.setItems(availablePolicyCategories);

        policyCategory.setItemLabelGenerator(
            category ->
                category.getCode()
                    + " - "
                    + category.getNameEn()
                    + " | "
                    + category.getNameKh()
        );

        policyCategory.setPlaceholder(
            "Select a category | ជ្រើសរើសប្រភេទ"
        );

        policyCategory.setRequiredIndicatorVisible(true);
    }

    /**
     * Vaadin Select identifies values using equals(). JPA may return a
     * different PolicyCategory instance for the saved policy than the
     * instance stored in the Select items. Match the saved category by ID
     * and give Binder the exact selectable instance.
     *
     * If an existing policy uses an obsolete category, include that category
     * for this edit session so its current value is still displayed.
     */
    private void preparePolicyCategoryForBinder() {
        List<PolicyCategory> editorCategories =
                new ArrayList<>(availablePolicyCategories);

        PolicyCategory savedCategory =
                this.entity.getPolicyCategory();

        if (savedCategory == null
                || savedCategory.getId() == null) {

            policyCategory.setItems(editorCategories);
            return;
        }

        Long savedCategoryId = savedCategory.getId();

        PolicyCategory selectableCategory =
                editorCategories.stream()
                    .filter(category ->
                        Objects.equals(
                            category.getId(),
                            savedCategoryId
                        )
                    )
                    .findFirst()
                    .orElseGet(() -> {
                        PolicyCategory existingCategory =
                                policyCategoryRepository
                                    .findById(savedCategoryId)
                                    .orElseThrow(() ->
                                        new IllegalArgumentException(
                                            "The policy category "
                                                + "no longer exists."
                                        )
                                    );

                        editorCategories.add(existingCategory);
                        return existingCategory;
                    });

        policyCategory.setItems(editorCategories);
        this.entity.setPolicyCategory(selectableCategory);
    }

    // ---------------------------------------------------------------------
    // Form lifecycle
    // ---------------------------------------------------------------------

    @Override
    protected void populateForm(
            Policy entityValue) throws Exception {

        if (entityValue != null
                && entityValue.getId() != null) {

            this.entity =
                    service.findById(entityValue.getId())
                        .orElseThrow(() ->
                            new IllegalArgumentException(
                                "This record no longer exists. "
                                    + "Please refresh the page."
                            )
                        );
        } else {
            this.entity =
                    entityValue != null
                        ? entityValue
                        : createNewEntity();
        }

        code.setReadOnly(true);
        policyCategory.setReadOnly(false);

        /*
         * Remember the original values so they can be restored
         * when the user selects the original category again.
         */
        originalPolicyCode = this.entity.getCode();

        originalPolicyCategoryId =
                this.entity.getPolicyCategory() != null
                    ? this.entity
                        .getPolicyCategory()
                        .getId()
                    : null;

        preparePolicyCategoryForBinder();
        binder.readBean(this.entity);

        fileAttachmentComponent.setOwnerId(
            this.entity.getId()
        );

        tabs.setSelectedIndex(0);
        detailTabContent.setVisible(true);
        attachmentTabContent.setVisible(false);

        editorLayout.open();
    }

    @Override
    protected void beforeSave(Policy entity,boolean isNew) throws Exception {
        validatePolicy(entity);
        fileAttachmentComponent.commitAllPendingEdits();
        fileAttachmentComponent.validateAttachments();
    }

    @Override
    protected void afterSave(Policy entity,boolean isNew) throws Exception {
        fileAttachmentComponent.saveChanges(entity.getId());
        Notification.show(isNew ? "Policy created successfully" : "Policy updated successfully", 3000,Notification.Position.MIDDLE);
    }

    private void validatePolicy(Policy entity) {
        if (entity.getCode() == null || entity.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Code is required | កូដត្រូវបានទាមទារ" );
        }

        if (entity.getTitleEn() == null || entity.getTitleEn().trim().isEmpty()) {
            throw new IllegalArgumentException("English title is required | " + "ចំណងជើងអង់គ្លេសត្រូវបានទាមទារ" );
        }

        if (entity.getTitleKh() == null || entity.getTitleKh().trim().isEmpty()) { 
        	throw new IllegalArgumentException( "Khmer title is required | " + "ចំណងជើងខ្មែរត្រូវបានទាមទារ" );
        }

        if (entity.getPolicyCategory() == null) {
            throw new IllegalArgumentException("Policy category is required | " + "ប្រភេទគោលនយោបាយត្រូវបានទាមទារ" );
        }

        if (entity.getEffectiveDate() == null) {
            throw new IllegalArgumentException("Effective date is required | "+ "ថ្ងៃចាប់ផ្តើមអនុវត្តត្រូវបានទាមទារ" );
        }

        if (entity.getExpiryDate() != null  && entity.getEffectiveDate() != null  && entity.getExpiryDate().isBefore( entity.getEffectiveDate()  )) {
            throw new IllegalArgumentException( "Expiry date must be after effective date | " + "ថ្ងៃផុតកំណត់ត្រូវតែក្រោយថ្ងៃចាប់ផ្តើមអនុវត្ត"
            );
        }
    }

    // ---------------------------------------------------------------------
    // Binder
    // ---------------------------------------------------------------------

    @Override
    protected void binderField() {    	
    	binder.bindInstanceFields(this);

        binder.forField(expiryDate)
            .withValidator(
                value ->
                    value == null
                        || effectiveDate.getValue() == null
                        || !value.isBefore(
                            effectiveDate.getValue()
                        ),
                "Expiry date must be on or after "
                    + "the effective date"
            )
            .bind(
                Policy::getExpiryDate,
                Policy::setExpiryDate
            );

 
    }

    @Override
    protected void focusFirstField() {
        code.focus();
    }

    @Override
    protected Policy createNewEntity() {
        Policy policy = new Policy();

        policy.setPublished(false);
        policy.setRequiredAcknowledge(false);
        policy.setActive(false);

        /*
         * Keep this only while Policy still contains the old
         * PolicyAttachment collection mapping.
         *
         * It may be removed later when PolicyAttachment is fully retired.
         */


        return policy;
    }

    // ---------------------------------------------------------------------
    // Main policy grid columns
    // ---------------------------------------------------------------------

    @Override
    protected List<ColumnDef<Policy>>
            getColumnDefs() {

        return new ArrayList<>(
            List.of(
                col(
                    "id",
                    "ID",
                    Policy::getId,
                    item ->
                        item.getId() != null
                            ? item.getId().toString()
                            : ""
                ),
                col(
                    "code",
                    code.getLabel(),
                    Policy::getCode,
                    item ->
                        item.getCode() != null
                            ? item.getCode()
                            : ""
                ),
                col(
                    "titleEn",
                    titleEn.getLabel(),
                    Policy::getTitleEn,
                    item ->
                        item.getTitleEn() != null
                            ? item.getTitleEn()
                            : ""
                ),
                col(
                    "titleKh",
                    titleKh.getLabel(),
                    Policy::getTitleKh,
                    item ->
                        item.getTitleKh() != null
                            ? item.getTitleKh()
                            : ""
                ),
                col(
                    "policyCategory",
                    "Category",
                    Policy::getPolicyCategory,
                    item ->
                        item.getPolicyCategory() != null
                            ? item
                                .getPolicyCategory()
                                .getNameEn()
                            : ""
                ),
                col(
                    "effectiveDate",
                    effectiveDate.getLabel(),
                    Policy::getEffectiveDate,
                    item ->
                        item.getEffectiveDate() != null
                            ? item
                                .getEffectiveDate()
                                .toString()
                            : ""
                ),
                col(
                    "expiryDate",
                    expiryDate.getLabel(),
                    Policy::getExpiryDate,
                    item ->
                        item.getExpiryDate() != null
                            ? item
                                .getExpiryDate()
                                .toString()
                            : ""
                ),
                col(
                    "published",
                    published.getLabel(),
                    Policy::isPublished,
                    item ->
                        item.isPublished()
                            ? "Yes"
                            : "No"
                ),
                col(
                    "active",
                    active.getLabel(),
                    Policy::isActive,
                    item ->
                        item.isActive()
                            ? "Yes"
                            : "No"
                )
            )
        );
    }

    // ---------------------------------------------------------------------
    // Advanced search
    // ---------------------------------------------------------------------

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel =
            new AdvancedSearchPanel(
                List.of(
                    new FilterDef(
                        "code",
                        code.getLabel(),
                        () -> createContainsField(),
                        component ->
                            ((TextField) component)
                                .clear()
                    ),
                    new FilterDef(
                        "titleEn",
                        titleEn.getLabel(),
                        () -> createContainsField(),
                        component ->
                            ((TextField) component)
                                .clear()
                    ),
                    new FilterDef(
                        "titleKh",
                        titleKh.getLabel(),
                        () -> createContainsField(),
                        component ->
                            ((TextField) component)
                                .clear()
                    ),
                    new FilterDef(
                        "policyCategory",
                        "Category",
                        this::createCategoryFilter,
                        component ->
                            ((Select<?>) component)
                                .clear()
                    ),
                    new FilterDef(
                        "published",
                        published.getLabel(),
                        this::createBooleanFilter,
                        component ->
                            ((Select<?>) component)
                                .clear()
                    ),
                    new FilterDef(
                        "active",
                        active.getLabel(),
                        this::createBooleanFilter,
                        component ->
                            ((Select<?>) component)
                                .clear()
                    )
                )
            );

        return advPanel;
    }

    private TextField createContainsField() {
        TextField field = new TextField();

        field.setPlaceholder("Contains...");
        field.setWidthFull();

        return field;
    }

    private Select<PolicyCategory>
            createCategoryFilter() {

        Select<PolicyCategory> select =
                new Select<>();

        select.setItems(
            policyCategoryRepository
                .findByObsoleteDateIsNullOrderByNameEnAsc()
        );

        select.setItemLabelGenerator(
            category ->
                category.getCode()
                    + " - "
                    + category.getNameEn()
        );

        select.setPlaceholder(
            "Select category..."
        );

        select.setWidthFull();

        return select;
    }

    private Select<Boolean>
            createBooleanFilter() {

        Select<Boolean> select =
                new Select<>();

        select.setItems(
            true,
            false
        );

        select.setItemLabelGenerator(
            value ->
                Boolean.TRUE.equals(value)
                    ? "Yes"
                    : "No"
        );

        select.setPlaceholder("Select...");
        select.setWidthFull();

        return select;
    }

    @Override
    protected Specification<Policy>
            buildCombinedSpecification() {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            List<String> sqlFilter =
                    new ArrayList<>();

            String quickSearch =
                    quickSearchField.getValue();

            if (quickSearch != null
                    && !quickSearch.isBlank()) {

                String like =
                        "%"
                            + quickSearch
                                .toLowerCase()
                                .trim()
                            + "%";

                predicates.add(
                    criteriaBuilder.or(
                        buildLikePredicate(
                            criteriaBuilder,
                            root.get("code"),
                            like
                        ),
                        buildLikePredicate(
                            criteriaBuilder,
                            root.get("titleEn"),
                            like
                        ),
                        buildLikePredicate(
                            criteriaBuilder,
                            root.get("titleKh"),
                            like
                        ),
                        buildLikePredicate(
                            criteriaBuilder,
                            root.get("summary"),
                            like
                        )
                    )
                );

                sqlFilter.add(
                    "Quick Search: "
                        + quickSearch.trim()
                );
            }

            /*
             * Keep this section consistent with the implementation used by
             * your AdvancedSearchPanel base class.
             *
             * The original PolicyView contained only a placeholder here, so
             * this replacement preserves that behavior.
             */
            if (advPanel != null) {
                // Add advanced-filter predicates here when required.
            }

            showSqlFilterTokens(sqlFilter);

            return criteriaBuilder.and(
                predicates.toArray(
                    new Predicate[0]
                )
            );
        };
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) {
            advPanel.clearAll();
        }
    }

    @Override
    protected Set<String>
            getAdditionalExcludedKeys() {

        /*
         * "attachments" remains excluded while Policy still contains the old
         * PolicyAttachment collection mapping.
         */
        return Set.of(
            "attachments",
            "acknowledgements",
            "policyCategory",
            "version"
        );
    }

    @Override
    protected boolean requiresPasswordConfirmation(
            SensitiveAction action) {

        return action == SensitiveAction.DELETE;
    }


    // ---------------------------------------------------------------------
    // Policy QR code and public sharing context menu
    // ---------------------------------------------------------------------

    private void configurePolicyContextMenu() {

        if (policyContextMenu != null) {
            return;
        }

        policyContextMenu = grid.addContextMenu();

        policyContextMenu.addItem(
            "🔳 Show QR Code | បង្ហាញ QR Code",
            event -> event.getItem().ifPresent(this::showQrCodeDialog)
        );

        policyContextMenu.addItem(
            "📋 Copy Link | ចម្លងតំណ",
            event -> event.getItem().ifPresent(this::copyPublicLink)
        );

        policyContextMenu.addItem(
            "🌐 Open View | បើកទំព័រ",
            event -> event.getItem().ifPresent(this::openPublicPolicy)
        );
    }

    private boolean validatePolicyForSharing(Policy policy) {
        if (policy == null || policy.getId() == null) {
            showShareWarning("Please save the policy before sharing | សូមរក្សាទុកគោលនយោបាយមុនពេលចែករំលែក");
            return false;
        }

        if (policy.getPublicToken() == null) {
            showShareWarning("Public token is missing. Save the policy again | មិនមានលេខសម្គាល់សាធារណៈទេ");
            return false;
        }

        if (!policy.isActive()) {
            showShareWarning("Only an active policy can be shared | អាចចែករំលែកបានតែគោលនយោបាយសកម្ម");
            return false;
        }

        if (!policy.isPublished()) {
            showShareWarning("Publish the policy before sharing | សូមចុះផ្សាយគោលនយោបាយមុនពេលចែករំលែក");
            return false;
        }

        java.time.LocalDate today = java.time.LocalDate.now();

        if (policy.getEffectiveDate() != null
                && policy.getEffectiveDate().isAfter(today)) {
            showShareWarning("The policy is not effective yet | គោលនយោបាយមិនទាន់មានប្រសិទ្ធភាពទេ");
            return false;
        }

        if (policy.getExpiryDate() != null
                && policy.getExpiryDate().isBefore(today)) {
            showShareWarning("The policy has expired | គោលនយោបាយបានផុតកំណត់");
            return false;
        }

        return true;
    }

    private void showShareWarning(String message) {
        Notification notification = Notification.show(
            message,
            4000,
            Notification.Position.MIDDLE
        );
        notification.addThemeVariants(
            com.vaadin.flow.component.notification.NotificationVariant.LUMO_WARNING
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

    private void copyPublicLink(Policy policy) {
        if (!validatePolicyForSharing(policy)) {
            return;
        }

        String url = buildPolicyReadUrl(policy.getPublicToken());

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
            url
        );

        Notification.show(
            "Public link copied | បានចម្លងតំណសាធារណៈ",
            2500,
            Notification.Position.MIDDLE
        );
    }

    private void openPublicPolicy(Policy policy) {
        if (!validatePolicyForSharing(policy)) {
            return;
        }
        UI ui = UI.getCurrent();
        ui.getPage().open(
        		buildPolicyReadUrl(policy.getPublicToken()),
                "_blank"
        );

    }



    private void showQrCodeDialog(Policy policy) {

        if (!validatePolicyForSharing(policy)) {
            return;
        }

        String publicUrl = buildPolicyReadUrl(policy.getPublicToken());

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

        String title = policy.getTitleEn();

        if (title == null || title.isBlank()) {
            title = policy.getCode();
        }

        H3 policyTitle = new H3(title);
        policyTitle.getStyle()
                .set("margin", "0")
                .set("text-align", "center")
                .set("overflow-wrap", "anywhere");

        Paragraph khmerTitle = new Paragraph(
                policy.getTitleKh() == null
                        ? ""
                        : policy.getTitleKh());

        khmerTitle.setVisible(
                policy.getTitleKh() != null
                        && !policy.getTitleKh().isBlank());

        khmerTitle.getStyle()
                .set("margin", "0")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        Image qrImage = new Image(
                qrCodeService.createStreamResource(publicUrl),
                "QR Code for " + title);

        qrImage.setWidth("300px");
        qrImage.setHeight("300px");

        qrImage.getStyle()
                .set("max-width", "80vw")
                .set("object-fit", "contain")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.5rem")
                .set("background", "white");

        Paragraph link = new Paragraph(publicUrl);

        link.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("overflow-wrap", "anywhere")
                .set("text-align", "center")
                .set("margin", "0");

        Button copyButton = new Button(
                "Copy Link | ចម្លងតំណ",
                event -> copyPublicLink(policy));

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

}
package org.halocambodia.views.policy;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PolicyAccessAction;
import org.halocambodia.data.PolicyAccessLog;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PolicyAccessLogService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "policy-access-logs", layout = MainLayout.class)
@PageTitle("Policy Access Logs")
@PermitAll
public class PolicyAccessLogView extends PageDialogLayout< PolicyAccessLog, PolicyAccessLogService> {


    private final TextField detailPolicy = createReadOnlyTextField("Policy | គោលនយោបាយ");

    private final TextField detailUser = createReadOnlyTextField("User | អ្នកប្រើប្រាស់");

    private final TextField detailAction =createReadOnlyTextField("Action | សកម្មភាព");

   // private final TextField detailAttachment = createReadOnlyTextField("Attachment | ឯកសារភ្ជាប់");

    private final TextField detailIpAddress =createReadOnlyTextField("IP Address");

    private final TextField detailAccessedAt =createReadOnlyTextField("Accessed At | ពេលវេលាចូលប្រើ");

    private final TextArea detailUserAgent = createReadOnlyTextArea("Browser / Device | កម្មវិធីរុករក / ឧបករណ៍");

    private boolean editorConfigured;

    public PolicyAccessLogView(
            PolicyAccessLogService service,
            UserService userService,
            AuthenticatedUser authenticatedUser) {

        super(
                PolicyAccessLog.class,
                service,
                userService,
                authenticatedUser
        );
    }

    /*
     * Configure the page after all subclass fields are initialized.
     */
    @Override
    protected void onViewAttachOnce( AttachEvent attachEvent) throws Exception {

        enableToggleColumn = false;
        toggleColumnFrozen = false;

        configureGrid();
        configureEditorLayout();
    }

    /*
     * Access logs are read-only. Keep the same toolbar as PolicyView,
     * but hide Add, Edit and Delete.
     */
    @Override
    protected void configureToolBar() {
        miAddNew.setVisible(false);
        miEdit.setVisible(false);
        miDelete.setVisible(false);

        miRefresh.setVisible(true);
        miAdvancedSearch.setVisible(true);
        miExportExcel.setVisible(true);
        miToggleColumns.setVisible(true);
    }

    /*
     * Use accessedAt instead of the PageDialogLayout default updatedAt.
     */
    @Override
    protected Sort getDefaultSort() {
        return Sort.by(
                Sort.Direction.DESC,
                "accessedAt"
        );
    }

    /*
     * Grid columns.
     */
    @Override
    protected List<ColumnDef<PolicyAccessLog>>
            getColumnDefs() {

        return new ArrayList<>(
                List.of(
                        col(
                                "id",
                                "ID",
                                PolicyAccessLog::getId,
                                log -> value(log.getId())
                        ),
                        col(
                                "policy.code",
                                "Policy | គោលនយោបាយ",
                                PolicyAccessLog::getPolicy,
                                this::formatPolicy
                        ),
                        col(
                                "user.name",
                                "User | អ្នកប្រើប្រាស់",
                                PolicyAccessLog::getUser,
                                log -> log.getUser() == null
                                        ? ""
                                        : value(log.getUser().getName())
                        ),
                        col(
                                "action",
                                "Action | សកម្មភាព",
                                PolicyAccessLog::getAction,
                                log -> formatAction(log.getAction())
                        ),
                        col(
                                "fileAttachment.fileName",
                                "Attachment | ឯកសារភ្ជាប់",
                                PolicyAccessLog::getFileAttachment,
                                log -> log.getFileAttachment() == null
                                        ? "—"
                                        : value(
                                                log.getFileAttachment()
                                                        .getFileName()
                                        )
                        ),
                        col(
                                "ipAddress",
                                "IP Address",
                                PolicyAccessLog::getIpAddress,
                                log -> value(log.getIpAddress())
                        ),
                        col(
                                "accessedAt",
                                "Accessed At | ពេលវេលាចូលប្រើ",
                                PolicyAccessLog::getAccessedAt,
                                log -> DateTimeUtilFormart.DATE_TIME_FORMATTER.format(
                                        log.getAccessedAt()
                                )
                        ),
                        col(
                                "userAgent",
                                "Browser / Device | កម្មវិធីរុករក / ឧបករណ៍",
                                PolicyAccessLog::getUserAgent,
                                log -> value(log.getUserAgent())
                        )
                )
        );
    }

    /*
     * Advanced-search controls.
     */
    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(
                List.of(
                        textFilter(
                                "policy",
                                "Policy code/title"
                        ),
                        textFilter(
                                "user",
                                "User"
                        ),
                        new FilterDef(
                                "action",
                                "Action",
                                this::createActionFilter,
                                component ->
                                        ((ComboBox<?>) component)
                                                .clear()
                        ),
                        textFilter(
                                "attachment",
                                "Attachment"
                        ),
                        textFilter(
                                "ipAddress",
                                "IP Address"
                        ),
                        textFilter(
                                "userAgent",
                                "Browser / Device"
                        ),
                        new FilterDef(
                                "accessedFrom",
                                "Accessed from",
                                this::createDateFilter,
                                component ->
                                        ((DatePicker) component)
                                                .clear()
                        ),
                        new FilterDef(
                                "accessedTo",
                                "Accessed to",
                                this::createDateFilter,
                                component ->
                                        ((DatePicker) component)
                                                .clear()
                        )
                )
        );

        return advPanel;
    }

    private FilterDef textFilter(
            String key,
            String label) {

        return new FilterDef(
                key,
                label,
                this::createContainsField,
                component ->
                        ((TextField) component).clear()
        );
    }

    private TextField createContainsField() {
        TextField field = new TextField();

        field.setPlaceholder("Contains...");
        field.setClearButtonVisible(true);
        field.setWidthFull();

        return field;
    }

    private ComboBox<PolicyAccessAction>
            createActionFilter() {

        ComboBox<PolicyAccessAction> field =
                new ComboBox<>();

        field.setItems(PolicyAccessAction.values());
        field.setItemLabelGenerator(
                this::formatAction
        );
        field.setPlaceholder("Select action...");
        field.setClearButtonVisible(true);
        field.setWidthFull();

        return field;
    }

    private DatePicker createDateFilter() {
        DatePicker field = new DatePicker();

        field.setClearButtonVisible(true);
        field.setWidthFull();

        return field;
    }

    /*
     * Quick search and advanced-search specification.
     */
    @Override
    protected Specification<PolicyAccessLog>
            buildCombinedSpecification() {

        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            List<Predicate> predicates =
                    new ArrayList<>();

            List<String> filterTokens =
                    new ArrayList<>();

            Join<?, ?> policyJoin =
                    root.join(
                            "policy",
                            JoinType.LEFT
                    );

            Join<?, ?> userJoin =
                    root.join(
                            "user",
                            JoinType.LEFT
                    );

            Join<?, ?> attachmentJoin =
                    root.join(
                            "fileAttachment",
                            JoinType.LEFT
                    );

            String quickSearch =
                    quickSearchField == null
                            ? ""
                            : value(quickSearchField.getValue())
                                    .trim();

            if (!quickSearch.isBlank()) {
                String like =
                        "%"
                                + quickSearch.toLowerCase()
                                + "%";

                predicates.add(
                        criteriaBuilder.or(
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("code"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("titleEn"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("titleKh"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        userJoin.get("name"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        attachmentJoin.get("fileName"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        root.get("ipAddress"),
                                        like
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        root.get("userAgent"),
                                        like
                                )
                        )
                );

                filterTokens.add(
                        "Quick Search: " + quickSearch
                );
            }

            if (advPanel != null) {
                addTextFilter(
                        predicates,
                        filterTokens,
                        "policy",
                        "Policy",
                        text -> criteriaBuilder.or(
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("code"),
                                        text
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("titleEn"),
                                        text
                                ),
                                buildLikePredicate(
                                        criteriaBuilder,
                                        policyJoin.get("titleKh"),
                                        text
                                )
                        )
                );

                addTextFilter(
                        predicates,
                        filterTokens,
                        "user",
                        "User",
                        text -> buildLikePredicate(
                                criteriaBuilder,
                                userJoin.get("name"),
                                text
                        )
                );

                addTextFilter(
                        predicates,
                        filterTokens,
                        "attachment",
                        "Attachment",
                        text -> buildLikePredicate(
                                criteriaBuilder,
                                attachmentJoin.get("fileName"),
                                text
                        )
                );

                addTextFilter(
                        predicates,
                        filterTokens,
                        "ipAddress",
                        "IP Address",
                        text -> buildLikePredicate(
                                criteriaBuilder,
                                root.get("ipAddress"),
                                text
                        )
                );

                addTextFilter(
                        predicates,
                        filterTokens,
                        "userAgent",
                        "Browser / Device",
                        text -> buildLikePredicate(
                                criteriaBuilder,
                                root.get("userAgent"),
                                text
                        )
                );

                ComboBox<PolicyAccessAction> actionField =
                        advPanel.getField(
                                "action",
                                ComboBox.class
                        );

                if (actionField != null
                        && actionField.getValue() != null) {

                    predicates.add(
                            criteriaBuilder.equal(
                                    root.get("action"),
                                    actionField.getValue()
                            )
                    );

                    filterTokens.add(
                            "Action: "
                                    + formatAction(
                                            actionField.getValue()
                                    )
                    );
                }

                DatePicker fromField =
                        advPanel.getField(
                                "accessedFrom",
                                DatePicker.class
                        );

                if (fromField != null
                        && fromField.getValue() != null) {

                    OffsetDateTime from = fromField.getValue().atStartOfDay(DateTimeUtilFormart.CAMBODIA_ZONE).toOffsetDateTime();

                    predicates.add(
                            criteriaBuilder.greaterThanOrEqualTo(
                                    root.<OffsetDateTime>get("accessedAt"),
                                    from
                            )
                    );

                    filterTokens.add(
                            "Accessed from: "
                                    + fromField.getValue()
                    );
                }

                DatePicker toField =
                        advPanel.getField(
                                "accessedTo",
                                DatePicker.class
                        );

                if (toField != null
                        && toField.getValue() != null) {

                    OffsetDateTime exclusiveEnd =
                            toField.getValue()
                                    .plusDays(1)
                                    .atStartOfDay(DateTimeUtilFormart.CAMBODIA_ZONE)
                                    .toOffsetDateTime();

                    predicates.add(
                            criteriaBuilder.lessThan(
                                    root.<OffsetDateTime>get("accessedAt"),
                                    exclusiveEnd
                            )
                    );

                    filterTokens.add(
                            "Accessed to: "
                                    + toField.getValue()
                    );
                }
            }

            showSqlFilterTokens(filterTokens);

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private void addTextFilter(
            List<Predicate> predicates,
            List<String> filterTokens,
            String key,
            String label,
            java.util.function.Function<String, Predicate> predicateFactory) {

        TextField field =
                advPanel.getField(
                        key,
                        TextField.class
                );

        if (field == null
                || field.getValue() == null
                || field.getValue().isBlank()) {

            return;
        }

        String text = field.getValue().trim();
        String like =
                "%"
                        + text.toLowerCase()
                        + "%";

        predicates.add(
                predicateFactory.apply(like)
        );

        filterTokens.add(
                label + ": " + text
        );
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) {
            advPanel.clearAll();
        }
    }

    /*
     * Build the read-only detail dialog used when a row is
     * double-clicked.
     */
    @Override
    protected void configureEditorLayout() {
        if (editorConfigured) {
            return;
        }
        editorConfigured = true;

        FormLayout form = new FormLayout();

        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep(
                        "0",
                        1
                ),
                new FormLayout.ResponsiveStep(
                        "650px",
                        2
                )
        );

        form.add(
                detailPolicy,
                detailUser,
                detailAction,
     
                detailIpAddress,
                detailAccessedAt,
                detailUserAgent
        );

        form.setColspan(
                detailUserAgent,
                2
        );

        Button close = new Button(
                "Close | បិទ",
                event -> editorLayout.close()
        );

        close.setIcon(
                new Icon(VaadinIcon.CLOSE)
        );

        close.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        HorizontalLayout footer =
                new HorizontalLayout(close);

        footer.addClassName("button-layout");

        editorLayout.add(form);
        editorLayout.getFooter().add(footer);
    }

    /*
     * Double-clicking a row opens a read-only detail dialog.
     */
    @Override
    protected void onEdit(
            PolicyAccessLog selected) {

        if (selected == null) {
            return;
        }

        entity = selected;

        editorLayout.setDialogTitle(
                "Policy Access Log – View"
        );

        try {
            populateForm(selected);
        } catch (Exception exception) {
            showErrorMessage(
                    "Unable to open access log: "
                            + exception.getMessage()
            );
        }
    }

    @Override
    protected void populateForm(
            PolicyAccessLog log) {

        detailPolicy.setValue(
                formatPolicy(log)
        );

        detailUser.setValue(
                log.getUser() == null
                        ? ""
                        : value(log.getUser().getName())
        );

        detailAction.setValue(
                formatAction(log.getAction())
        );



        detailIpAddress.setValue(
                value(log.getIpAddress())
        );

        detailAccessedAt.setValue(
        		DateTimeUtilFormart.DATE_TIME_FORMATTER.format(log.getAccessedAt())
        );

        detailUserAgent.setValue(
                value(log.getUserAgent())
        );

        editorLayout.open();
    }

    @Override
    protected PolicyAccessLog createNewEntity() {
        return new PolicyAccessLog();
    }

    @Override
    protected void focusFirstField() {
        // Read-only view: no field receives edit focus.
    }

    @Override
    protected void binderField() {
        // Read-only view: no binder is required.
    }

    @Override
    protected boolean requiresPasswordConfirmation(
            SensitiveAction action) {

        return false;
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of(
                "userAgent"
        );
    }

    @Override
    protected String getEntityLabelSingular() {
        return "policy access log";
    }

    @Override
    protected String getEntityLabelPlural() {
        return "policy access logs";
    }

    private static TextField createReadOnlyTextField(
            String label) {

        TextField field = new TextField(label);

        field.setReadOnly(true);
        field.setWidthFull();

        return field;
    }

    private static TextArea createReadOnlyTextArea(
            String label) {

        TextArea field = new TextArea(label);

        field.setReadOnly(true);
        field.setWidthFull();
        field.setMinHeight("100px");

        return field;
    }

    private String formatPolicy(
            PolicyAccessLog log) {

        if (log == null
                || log.getPolicy() == null) {

            return "";
        }

        String code =
                value(log.getPolicy().getCode());

        String title =
                value(log.getPolicy().getTitleEn());

        if (code.isBlank()) {
            return title;
        }

        if (title.isBlank()) {
            return code;
        }

        return code + " - " + title;
    }

    private String formatAction(
            PolicyAccessAction action) {

        if (action == null) {
            return "";
        }

        String text =
                action.name()
                        .toLowerCase()
                        .replace('_', ' ');

        return Character.toUpperCase(
                text.charAt(0)
        ) + text.substring(1);
    }



    private static String value(
            Object value) {

        return value == null
                ? ""
                : value.toString();
    }
}

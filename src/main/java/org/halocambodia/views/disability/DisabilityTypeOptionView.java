package org.halocambodia.views.disability;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.DisabilityType;
import org.halocambodia.data.DisabilityTypeOption;
import org.halocambodia.data.DisabilityTypeRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.DisabilityOptionService;

import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "disability-type-options", layout = MainLayout.class)
@PageTitle("Disability Type Options | ជម្រើសប្រភេទពិការភាព")
@PermitAll
public class DisabilityTypeOptionView extends PageDialogLayout<DisabilityTypeOption, DisabilityOptionService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // ✅ use repository to load DisabilityType list
    private final DisabilityTypeRepository disabilityTypeRepository;

    // ===================== Editor fields =====================
    private final ComboBox<DisabilityType> disabilityType = new ComboBox<>("Disability Type | ប្រភេទពិការភាព");
    private final TextField disabilityTypeOptionName = new TextField("Disability Option | ជម្រើសពិការភាព");
    private final TextArea note = new TextArea("Note | កំណត់សម្គាល់");

    public DisabilityTypeOptionView(DisabilityOptionService service,
                                    DisabilityTypeRepository disabilityTypeRepository,
                                    UserService userService,
                                    AuthenticatedUser authenticatedUser,
                                    PasswordEncoder passwordEncoder) {
        super(DisabilityTypeOption.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.passwordEncoder = passwordEncoder;
        this.disabilityTypeRepository = disabilityTypeRepository;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {

        disabilityType.setWidthFull();
        disabilityType.setPlaceholder("Select disability type... | ជ្រើសរើសប្រភេទពិការភាព...");
        disabilityType.setClearButtonVisible(true);
        disabilityType.setItemLabelGenerator(d -> d == null ? "" : d.getDisabilityTypeName());
        disabilityType.setRenderer(new TextRenderer<>(d -> d == null ? "" : d.getDisabilityTypeName()));
        disabilityType.setItems(disabilityTypeRepository.findAll(Sort.by("disabilityTypeName")));

        disabilityTypeOptionName.setWidthFull();
        disabilityTypeOptionName.setPlaceholder("Enter option... | បញ្ចូលជម្រើស...");
        disabilityTypeOptionName.setClearButtonVisible(true);

        note.setWidthFull();
        note.setPlaceholder("Optional... | ជាជម្រើស...");
        note.setClearButtonVisible(true);
        note.setMinHeight("110px");
    }

    // ===================== Layout =====================

    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout form = new FormLayout(disabilityType, disabilityTypeOptionName, note);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        form.setColspan(note, 2);

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== CRUD hooks =====================

    @Override
    protected void populateForm(DisabilityTypeOption entity) throws Exception {
        this.entity = entity;


        // reload type list (safe if new types added)
        disabilityType.setItems(disabilityTypeRepository.findAll(Sort.by("disabilityTypeName")));
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected DisabilityTypeOption createNewEntity() {
        return new DisabilityTypeOption();
    }

    @Override
    protected void focusFirstField() {
        disabilityType.focus();
    }

    @Override
    protected void binderField() {

        binder.forField(disabilityType)
                .asRequired("Disability Type is required | ត្រូវការប្រភេទពិការភាព")
                .bind(DisabilityTypeOption::getDisabilityType, DisabilityTypeOption::setDisabilityType);

        binder.forField(disabilityTypeOptionName)
                .asRequired("Disability Option is required | ត្រូវការជម្រើសពិការភាព")
                .bind(DisabilityTypeOption::getDisabilityTypeOptionName, DisabilityTypeOption::setDisabilityTypeOptionName);

        binder.bind(note, DisabilityTypeOption::getNote, DisabilityTypeOption::setNote);
    }

    // ===================== Grid columns =====================

    @Override
    protected List<ColumnDef<DisabilityTypeOption>> getColumnDefs() {
        return List.of(
                col("id", "ID",
                        DisabilityTypeOption::getId,
                        e -> e.getId() == null ? "" : e.getId().toString()),

                col("disabilityType.disabilityTypeName", disabilityType.getLabel(),
                        e -> e.getDisabilityType() != null ? e.getDisabilityType().getDisabilityTypeName() : "",
                        e -> e.getDisabilityType() != null ? e.getDisabilityType().getDisabilityTypeName() : ""),

                col("disabilityTypeOptionName", disabilityTypeOptionName.getLabel(),
                        DisabilityTypeOption::getDisabilityTypeOptionName,
                        e -> e.getDisabilityTypeOptionName() == null ? "" : e.getDisabilityTypeOptionName()),

                col("note", note.getLabel(),
                        DisabilityTypeOption::getNote,
                        e -> e.getNote() == null ? "" : e.getNote()),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        DisabilityTypeOption::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        DisabilityTypeOption::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== Filtering =====================

    @Override
    protected Specification<DisabilityTypeOption> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<DisabilityTypeOption, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<DisabilityTypeOption, User> userUpdated = root.join("userUpdated", JoinType.LEFT);

                Join<DisabilityTypeOption, DisabilityType> typeJoin = root.join("disabilityType", JoinType.LEFT);

                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, cb.lower(typeJoin.get("disabilityTypeName")), like),
                            buildLikePredicate(cb, cb.lower(root.get("disabilityTypeOptionName")), like),
                            buildLikePredicate(cb, cb.lower(root.get("note")), like),
                            buildLikePredicate(cb, cb.lower(userCreated.get("name")), like),
                            buildLikePredicate(cb, cb.lower(userUpdated.get("name")), like)
                    ));
                }

                // Advanced filter
                if (advPanel != null) {

                    IntegerField idField = advPanel.getField("id", IntegerField.class);
                    if (idField != null && idField.getValue() != null) {
                        Long idVal = idField.getValue().longValue();
                        predicates.add(cb.equal(root.get("id"), idVal));
                        sqlFilter.add("ID = " + idVal);
                    }

                    @SuppressWarnings("unchecked")
                    ComboBox<DisabilityType> typeField =
                            (ComboBox<DisabilityType>) advPanel.getField("disabilityType", ComboBox.class);
                    if (typeField != null && typeField.getValue() != null) {
                        predicates.add(cb.equal(root.get("disabilityType"), typeField.getValue()));
                        sqlFilter.add(disabilityType.getLabel() + " = " + typeField.getValue().getDisabilityTypeName());
                    }

                    TextField optField = advPanel.getField("disabilityTypeOptionName", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("disabilityTypeOptionName"),
                            optField == null ? null : optField.getValue(),
                            disabilityTypeOptionName.getLabel(), predicates, sqlFilter);

                    TextField noteField = advPanel.getField("note", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("note"),
                            noteField == null ? null : noteField.getValue(),
                            note.getLabel(), predicates, sqlFilter);

                    DateRangePicker createdRange = advPanel.getField("createdAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,
                            cb.function("DATE", LocalDate.class, root.get("createdAt")),
                            createdRange == null ? null : createdRange.getFrom(),
                            createdRange == null ? null : createdRange.getTo(),
                            "Created At", predicates, sqlFilter);

                    DateRangePicker updatedRange = advPanel.getField("updatedAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,
                            cb.function("DATE", LocalDate.class, root.get("updatedAt")),
                            updatedRange == null ? null : updatedRange.getFrom(),
                            updatedRange == null ? null : updatedRange.getTo(),
                            "Updated At", predicates, sqlFilter);
                }

                showSqlFilterTokens(sqlFilter);
                return cb.and(predicates.toArray(new Predicate[0]));
            } catch (Exception ex) {
                showErrorMessage("Error in filter: " + ex.getMessage());
                ex.printStackTrace();
                return cb.conjunction();
            }
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {

        advPanel = new AdvancedSearchPanel(List.of(
                new AdvancedSearchPanel.FilterDef(
                        "id",
                        "ID",
                        () -> {
                            IntegerField tf = new IntegerField();
                            tf.setPlaceholder("Equal | ស្មើ");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((IntegerField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "disabilityType",
                        disabilityType.getLabel(),
                        () -> {
                            ComboBox<DisabilityType> cb = new ComboBox<>();
                            cb.setWidthFull();
                            cb.setClearButtonVisible(true);
                            cb.setPlaceholder("Select... | ជ្រើសរើស...");
                            cb.setItemLabelGenerator(d -> d == null ? "" : d.getDisabilityTypeName());
                            cb.setItems(disabilityTypeRepository.findAll(Sort.by("disabilityTypeName")));
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "disabilityTypeOptionName",
                        disabilityTypeOptionName.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "note",
                        note.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "createdAt",
                        "Created At | កាលបរិច្ឆេទបង្កើត",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "updatedAt",
                        "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                )
        ));

        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }

    // ===================== Password confirmation for delete =====================

    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE; // ✅ only delete
    }

    @Override
    protected void confirmPassword(SensitiveAction action, Runnable onSuccess) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Confirm Password");

        PasswordField pwd = new PasswordField("Password");
        pwd.setWidthFull();
        pwd.setRevealButtonVisible(true);
        pwd.setRequired(true);

        Span msg = new Span("Please enter your password to continue.");
        msg.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Button cancel = new Button("Cancel", e -> dlg.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button confirm = new Button("Confirm", e -> {
            String raw = pwd.getValue();
            if (raw == null || raw.isBlank()) {
                pwd.setInvalid(true);
                pwd.setErrorMessage("Password is required");
                return;
            }

            User u = currentUserLogin.orElse(null);
            if (u == null || u.getHashedPassword() == null) {
                Notification.show("Cannot detect current user/password", 2500, Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            boolean ok = passwordEncoder.matches(raw, u.getHashedPassword());
            if (!ok) {
                pwd.setInvalid(true);
                pwd.setErrorMessage("Wrong password");
                return;
            }

            dlg.close();
            onSuccess.run();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        confirm.addClickShortcut(Key.ENTER);
        cancel.addClickShortcut(Key.ESCAPE);

        VerticalLayout body = new VerticalLayout(msg, pwd);
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();

        dlg.add(body);
        dlg.getFooter().add(cancel, confirm);

        dlg.open();
        UI.getCurrent().access(pwd::focus);
    }
}

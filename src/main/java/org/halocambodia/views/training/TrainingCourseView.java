package org.halocambodia.views.training;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.TrainingCourse;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.TrainingCourseService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "training-courses", layout = MainLayout.class)
@PageTitle("Training Courses | វគ្គបណ្តុះបណ្តាល")
@PermitAll
public class TrainingCourseView extends PageDialogLayout<TrainingCourse, TrainingCourseService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // ===================== Editor fields =====================
    private final TextField trainingCourseName = new TextField("Training Course | វគ្គបណ្តុះបណ្តាល");
    private final Checkbox isActive = new Checkbox("Active | សកម្ម");
    private final TextArea remark = new TextArea("Remark | កំណត់សម្គាល់");

    public TrainingCourseView(TrainingCourseService service, UserService userService, AuthenticatedUser authenticatedUser,PasswordEncoder passwordEncoder) {
        super(TrainingCourse.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.passwordEncoder=passwordEncoder;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {
        trainingCourseName.setWidthFull();
        trainingCourseName.setPlaceholder("Enter course name... | បញ្ចូលឈ្មោះវគ្គ...");
        trainingCourseName.setClearButtonVisible(true);

        isActive.setValue(true);

        remark.setWidthFull();
        remark.setMinHeight("120px");
        remark.setPlaceholder("Notes... | កំណត់សម្គាល់...");
    }

    // ===================== Layout =====================

    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout form = new FormLayout(trainingCourseName, isActive, remark);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );
        

        // remark full width on 2-col layout
        form.setColspan(remark, 2);

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== CRUD hooks =====================

    @Override
    protected void populateForm(TrainingCourse entity) throws Exception {
        this.entity = entity;
        binder.readBean(entity);

        // default for new
        if (entity.getId() == null && entity.getIsActive() == null) {
            isActive.setValue(true);
        }

        editorLayout.open();
    }

    @Override
    protected TrainingCourse createNewEntity() {
        TrainingCourse tc = new TrainingCourse();
        tc.setIsActive(Boolean.TRUE);
        return tc;
    }

    @Override
    protected void focusFirstField() {
        trainingCourseName.focus();
    }

    @Override
    protected void binderField() {

        binder.forField(trainingCourseName)
                .asRequired("Training Course is required | ត្រូវការវគ្គបណ្តុះបណ្តាល")
                .bind(TrainingCourse::getTrainingCourseName, TrainingCourse::setTrainingCourseName);

        binder.forField(isActive)
                .bind(
                        e -> Boolean.TRUE.equals(e.getIsActive()),
                        (e, v) -> e.setIsActive(Boolean.TRUE.equals(v))
                );

        binder.bind(remark, TrainingCourse::getRemark, TrainingCourse::setRemark);
    }

    // ===================== Grid columns =====================

    @Override
    protected List<ColumnDef<TrainingCourse>> getColumnDefs() {
        return List.of(
                col("id", "ID", TrainingCourse::getId, e -> e.getId() == null ? "" : e.getId().toString()),

                col("trainingCourseName", trainingCourseName.getLabel(),
                        TrainingCourse::getTrainingCourseName,
                        e -> e.getTrainingCourseName() == null ? "" : e.getTrainingCourseName()),

                col("isActive", isActive.getLabel(),
                        TrainingCourse::getIsActive,
                        e -> Boolean.TRUE.equals(e.getIsActive()) ? "Yes | បាទ" : "No | ទេ"),

                col("remark", remark.getLabel(),
                        TrainingCourse::getRemark,
                        e -> e.getRemark() == null ? "" : e.getRemark()),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        TrainingCourse::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        TrainingCourse::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== Filtering =====================

    @Override
    protected Specification<TrainingCourse> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<TrainingCourse, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<TrainingCourse, User> userUpdated = root.join("userUpdated", JoinType.LEFT);

                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, cb.lower(root.get("trainingCourseName")), like),
                            buildLikePredicate(cb, cb.lower(root.get("remark")), like),
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

                    TextField nameField = advPanel.getField("trainingCourseName", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("trainingCourseName"),
                            nameField == null ? null : nameField.getValue(),
                            trainingCourseName.getLabel(), predicates, sqlFilter);

                    TextField remarkField = advPanel.getField("remark", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("remark"),
                            remarkField == null ? null : remarkField.getValue(),
                            remark.getLabel(), predicates, sqlFilter);

                    Checkbox activeField = advPanel.getField("isActive", Checkbox.class);
                    if (activeField != null && activeField.getValue() != null) {
                        predicates.add(cb.equal(root.get("isActive"), activeField.getValue()));
                        sqlFilter.add(isActive.getLabel() + " = " + (activeField.getValue() ? "TRUE" : "FALSE"));
                    }

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> createdByMs = (MultiSelectComboBox<User>) advPanel.getField("userCreated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userCreated,
                            createdByMs == null ? null : createdByMs.getValue(),
                            User::getName, "Created By", predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> updatedByMs = (MultiSelectComboBox<User>) advPanel.getField("userUpdated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userUpdated,
                            updatedByMs == null ? null : updatedByMs.getValue(),
                            User::getName, "Updated By", predicates, sqlFilter);

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
                        "trainingCourseName",
                        trainingCourseName.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "remark",
                        remark.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "isActive",
                        isActive.getLabel(),
                        () -> {
                            Checkbox cb = new Checkbox("Active only | សកម្មតែប៉ុណ្ណោះ");
                            cb.setValue(true); // default filter
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((Checkbox) c).setValue(true)
                ),

                new AdvancedSearchPanel.FilterDef(
                        "userCreated",
                        "Created By | បង្កើតដោយ",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
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
                        "userUpdated",
                        "Updated By | កែប្រែដោយ",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
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

        advPanel.addFilter("name");
        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }
    
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

        // Enter key = confirm
        confirm.addClickShortcut(Key.ENTER);
        cancel.addClickShortcut(Key.ESCAPE);

        VerticalLayout body = new VerticalLayout(msg, pwd);
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();

        dlg.add(body);
        dlg.getFooter().add(cancel, confirm);

        dlg.open();

        // focus password field
        UI.getCurrent().access(pwd::focus);
    }
}

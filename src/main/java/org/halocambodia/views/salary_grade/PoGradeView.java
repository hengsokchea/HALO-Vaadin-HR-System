package org.halocambodia.views.salary_grade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.PoGrade;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PoGradeService;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;


@Route(value = "po-grade", layout = MainLayout.class)
@PageTitle("Salary Grades | កម្រិតប្រាក់ខែ")
@PermitAll
public class PoGradeView extends PageDialogLayout<PoGrade, PoGradeService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // Editor fields
    private final TextField poGradeField = new TextField("Salary Grades | កម្រិតប្រាក់ខែ");

    public PoGradeView(PoGradeService service, AuthenticatedUser authenticatedUser,PasswordEncoder passwordEncoder) {
        super(PoGrade.class, service, null, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {
        poGradeField.setWidthFull();
        poGradeField.setPlaceholder("Enter PO Grade | បញ្ចូលចំណាត់ថ្នាក់...");
        poGradeField.setClearButtonVisible(true);
    }

    // ===================== Layout =====================
    @Override
    protected void configureEditorLayout() throws Exception {
        FormLayout form = new FormLayout(poGradeField);
        form.setWidthFull();
       
        editorLayout.add(new VerticalLayout(form));
        form.setColspan(poGradeField, 2);
        configureEditorFooter();
    }

    // ===================== CRUD hooks =====================
    @Override
    protected void populateForm(PoGrade entity) throws Exception {
        this.entity = entity;
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected PoGrade createNewEntity() {
        return new PoGrade();
    }

    @Override
    protected void focusFirstField() {
        poGradeField.focus();
    }

    @Override
    protected void binderField() {
        binder.forField(poGradeField)
                .asRequired("PO Grade is required | ត្រូវការចំណាត់ថ្នាក់")
                .withValidator(new StringLengthValidator(
                        "PO Grade must be between 1 and 100 characters",
                        1, 100))
                .bind(PoGrade::getPoGrade, PoGrade::setPoGrade);
    }

    // ===================== Grid columns =====================
    @Override
    protected List<ColumnDef<PoGrade>> getColumnDefs() {
        return List.of(
                col("id", "ID",
                        PoGrade::getId,
                        e -> e.getId() == null ? "" : e.getId().toString()),

                col("poGrade", poGradeField.getLabel(),
                        PoGrade::getPoGrade,
                        PoGrade::getPoGrade),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        PoGrade::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        PoGrade::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== Filtering =====================
    @Override
    protected Specification<PoGrade> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.like(cb.lower(root.get("poGrade")), like));
                }

                // Advanced filter
                if (advPanel != null) {
                    TextField poGradeFilter = advPanel.getField("poGrade", TextField.class);
                    if (poGradeFilter != null && poGradeFilter.getValue() != null && !poGradeFilter.getValue().isBlank()) {
                        String val = "%" + poGradeFilter.getValue().toLowerCase() + "%";
                        predicates.add(cb.like(cb.lower(root.get("poGrade")), val));
                        sqlFilter.add("PO Grade LIKE " + poGradeFilter.getValue());
                    }
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
                        "poGrade",
                        poGradeField.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                )
        ));
        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }

    // ===================== Delete confirmation =====================
    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE;
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
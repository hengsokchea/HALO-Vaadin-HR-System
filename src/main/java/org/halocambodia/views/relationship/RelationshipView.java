package org.halocambodia.views.relationship;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Relationship;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.RelationshipService;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "relationships", layout = MainLayout.class)
@PageTitle("Relationships | ទំនាក់ទំនង")
@PermitAll
public class RelationshipView extends PageDialogLayout<Relationship, RelationshipService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // ===================== Editor fields =====================
    private final TextField relationshipEn = new TextField("Relationship (En) | ទំនាក់ទំនង (អង់គ្លេស)");
    private final TextField relationshipKh = new TextField("Relationship (Kh) | ទំនាក់ទំនង (ខ្មែរ)");

    public RelationshipView(RelationshipService service,
                            UserService userService,
                            AuthenticatedUser authenticatedUser,
                            PasswordEncoder passwordEncoder) {
        super(Relationship.class, service, userService, authenticatedUser);
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
        relationshipEn.setWidthFull();
        relationshipEn.setPlaceholder("Enter relationship... | បញ្ចូលទំនាក់ទំនង...");
        relationshipEn.setClearButtonVisible(true);

        relationshipKh.setWidthFull();
        relationshipKh.setPlaceholder("Enter Khmer relationship... | បញ្ចូលទំនាក់ទំនងខ្មែរ...");
        relationshipKh.setClearButtonVisible(true);
    }

    // ===================== Layout =====================

    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout form = new FormLayout(relationshipEn, relationshipKh);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== CRUD hooks =====================

    @Override
    protected void populateForm(Relationship entity) throws Exception {
        this.entity = entity;
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected Relationship createNewEntity() {
        return new Relationship();
    }

    @Override
    protected void focusFirstField() {
        relationshipEn.focus();
    }

    @Override
    protected void binderField() {

        binder.forField(relationshipEn)
                .asRequired("Relationship EN is required | ត្រូវការទំនាក់ទំនង(EN)")
                .bind(Relationship::getRelationshipEn, Relationship::setRelationshipEn);

        binder.bind(relationshipKh, Relationship::getRelationshipKh, Relationship::setRelationshipKh);
    }

    // ===================== Grid columns =====================

    @Override
    protected List<ColumnDef<Relationship>> getColumnDefs() {
        return List.of(
                col("id", "ID",
                        Relationship::getId,
                        e -> e.getId() == null ? "" : e.getId().toString()),

                col("relationshipEn", relationshipEn.getLabel(),
                        Relationship::getRelationshipEn,
                        e -> e.getRelationshipEn() == null ? "" : e.getRelationshipEn()),

                col("relationshipKh", relationshipKh.getLabel(),
                        Relationship::getRelationshipKh,
                        e -> e.getRelationshipKh() == null ? "" : e.getRelationshipKh()),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        Relationship::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        Relationship::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== Filtering =====================

    @Override
    protected Specification<Relationship> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<Relationship, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<Relationship, User> userUpdated = root.join("userUpdated", JoinType.LEFT);

                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, cb.lower(root.get("relationshipEn")), like),
                            buildLikePredicate(cb, cb.lower(root.get("relationshipKh")), like),
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

                    TextField enField = advPanel.getField("relationshipEn", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("relationshipEn"),
                            enField == null ? null : enField.getValue(),
                            relationshipEn.getLabel(), predicates, sqlFilter);

                    TextField khField = advPanel.getField("relationshipKh", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("relationshipKh"),
                            khField == null ? null : khField.getValue(),
                            relationshipKh.getLabel(), predicates, sqlFilter);

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
                        "relationshipEn",
                        relationshipEn.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "relationshipKh",
                        relationshipKh.getLabel(),
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

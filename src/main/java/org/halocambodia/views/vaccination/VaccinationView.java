// ============================
// VaccinationView
// ============================
package org.halocambodia.views.vaccination;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.User;
import org.halocambodia.data.Vaccination;
import org.halocambodia.data.VaccinationType;
import org.halocambodia.data.VaccinationTypeRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.UserService;
import org.halocambodia.services.VaccinationService;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "vaccinations", layout = MainLayout.class)
@PageTitle("Vaccinations | វ៉ាក់សាំង")
@PermitAll
public class VaccinationView extends PageDialogLayout<Vaccination, VaccinationService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // ✅ use repository to load vaccination types
    private final VaccinationTypeRepository vaccinationTypeRepository;

    // ===================== Editor fields =====================
    private final TextField vaccineName = new TextField("Vaccine Name | ឈ្មោះវ៉ាក់សាំង");
    private final ComboBox<VaccinationType> vaccinationType = new ComboBox<>("Vaccination Type | ប្រភេទវ៉ាក់សាំង");

    public VaccinationView(VaccinationService service,
                           VaccinationTypeRepository vaccinationTypeRepository,
                           UserService userService,
                           AuthenticatedUser authenticatedUser,
                           PasswordEncoder passwordEncoder) {
        super(Vaccination.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.passwordEncoder = passwordEncoder;
        this.vaccinationTypeRepository = vaccinationTypeRepository;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {
        vaccineName.setWidthFull();
        vaccineName.setPlaceholder("Enter vaccine name... | បញ្ចូលឈ្មោះវ៉ាក់សាំង...");
        vaccineName.setClearButtonVisible(true);

        vaccinationType.setWidthFull();
        vaccinationType.setPlaceholder("Select type... | ជ្រើសរើសប្រភេទ...");
        vaccinationType.setClearButtonVisible(true);
        vaccinationType.setItemLabelGenerator(t -> t == null ? "" : t.getVaccineTypeName());
        vaccinationType.setRenderer(new TextRenderer<>(t -> t == null ? "" : t.getVaccineTypeName()));
        vaccinationType.setItems(vaccinationTypeRepository.findAll(Sort.by("vaccineTypeName")));
    }

    @Override
    protected void configureEditorLayout() throws Exception {
        FormLayout form = new FormLayout(vaccineName, vaccinationType);
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    @Override
    protected void populateForm(Vaccination entity) throws Exception {
        this.entity = entity;

        vaccinationType.setItems(vaccinationTypeRepository.findAll(Sort.by("vaccineTypeName")));
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected Vaccination createNewEntity() {
        return new Vaccination();
    }

    @Override
    protected void focusFirstField() {
        vaccineName.focus();
    }

    @Override
    protected void binderField() {
        binder.forField(vaccineName)
                .asRequired("Vaccine Name is required | ត្រូវការឈ្មោះវ៉ាក់សាំង")
                .bind(Vaccination::getVaccineName, Vaccination::setVaccineName);

        binder.forField(vaccinationType)
                .asRequired("Vaccination Type is required | ត្រូវការប្រភេទវ៉ាក់សាំង")
                .bind(Vaccination::getVaccinationType, Vaccination::setVaccinationType);
    }

    @Override
    protected List<ColumnDef<Vaccination>> getColumnDefs() {
        return List.of(
                col("id", "ID", Vaccination::getId, e -> e.getId() == null ? "" : e.getId().toString()),

                col("vaccineName", vaccineName.getLabel(),
                        Vaccination::getVaccineName,
                        e -> e.getVaccineName() == null ? "" : e.getVaccineName()),

                col("vaccinationType.vaccineTypeName", vaccinationType.getLabel(),
                        e -> e.getVaccinationType() != null ? e.getVaccinationType().getVaccineTypeName() : "",
                        e -> e.getVaccinationType() != null ? e.getVaccinationType().getVaccineTypeName() : ""),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        Vaccination::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        Vaccination::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    @Override
    protected Specification<Vaccination> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<Vaccination, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<Vaccination, User> userUpdated = root.join("userUpdated", JoinType.LEFT);
                Join<Vaccination, VaccinationType> typeJoin = root.join("vaccinationType", JoinType.LEFT);

                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, cb.lower(root.get("vaccineName")), like),
                            buildLikePredicate(cb, cb.lower(typeJoin.get("vaccineTypeName")), like),
                            buildLikePredicate(cb, cb.lower(userCreated.get("name")), like),
                            buildLikePredicate(cb, cb.lower(userUpdated.get("name")), like)
                    ));
                }

                if (advPanel != null) {
                    IntegerField idField = advPanel.getField("id", IntegerField.class);
                    if (idField != null && idField.getValue() != null) {
                        Long idVal = idField.getValue().longValue();
                        predicates.add(cb.equal(root.get("id"), idVal));
                        sqlFilter.add("ID = " + idVal);
                    }

                    TextField nameField = advPanel.getField("vaccineName", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("vaccineName"),
                            nameField == null ? null : nameField.getValue(),
                            vaccineName.getLabel(), predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    ComboBox<VaccinationType> typeField =
                            (ComboBox<VaccinationType>) advPanel.getField("vaccinationType", ComboBox.class);
                    if (typeField != null && typeField.getValue() != null) {
                        predicates.add(cb.equal(root.get("vaccinationType"), typeField.getValue()));
                        sqlFilter.add(vaccinationType.getLabel() + " = " + typeField.getValue().getVaccineTypeName());
                    }

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
                        "id", "ID",
                        () -> {
                            IntegerField tf = new IntegerField();
                            tf.setPlaceholder("Equal | ស្មើ");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((IntegerField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "vaccineName", vaccineName.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains... | មានពាក្យ...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "vaccinationType", vaccinationType.getLabel(),
                        () -> {
                            ComboBox<VaccinationType> cb = new ComboBox<>();
                            cb.setWidthFull();
                            cb.setClearButtonVisible(true);
                            cb.setPlaceholder("Select type... | ជ្រើសរើសប្រភេទ...");
                            cb.setItemLabelGenerator(t -> t == null ? "" : t.getVaccineTypeName());
                            cb.setItems(vaccinationTypeRepository.findAll(Sort.by("vaccineTypeName")));
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
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

            if (!passwordEncoder.matches(raw, u.getHashedPassword())) {
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

        dlg.add(new VerticalLayout(msg, pwd));
        dlg.getFooter().add(cancel, confirm);
        dlg.open();
        UI.getCurrent().access(pwd::focus);
    }
}

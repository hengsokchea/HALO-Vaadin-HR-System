package org.halocambodia.views.donors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Donors;
import org.halocambodia.data.PoGrade;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.DonorService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route(value = "donors", layout = MainLayout.class)
@PageTitle("Donors | អ្នកឧបត្ថម្ភ")
@PermitAll
public class DonorView extends PageDialogLayout<Donors, DonorService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    // ===================== Fields =====================
    private final TextField donorLongName = new TextField("Donor Long Name");
    private final TextField donorShortName = new TextField("Donor Short Name");
    private final TextField donorCountry = new TextField("Country");

    public DonorView(DonorService service,
                      AuthenticatedUser authenticatedUser,
                      PasswordEncoder passwordEncoder) {
        super(Donors.class, service, null, authenticatedUser);
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
        donorLongName.setWidthFull();
        donorShortName.setWidthFull();
        donorCountry.setWidthFull();
    }

    // ===================== Layout =====================
    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout form = new FormLayout(
                donorLongName,
                donorShortName,
                donorCountry
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== Binder =====================
    @Override
    protected void binderField() {

        binder.forField(donorLongName)
                .asRequired("Required")
                .withValidator(new StringLengthValidator("Max 255", 1, 255))
                .bind(Donors::getDonorLongName, Donors::setDonorLongName);

        binder.forField(donorShortName)
                .asRequired("Required")
                .withValidator(new StringLengthValidator("Max 100", 1, 100))
                .bind(Donors::getDonorShortName, Donors::setDonorShortName);

        binder.forField(donorCountry)
                .asRequired("Required")
                .bind(Donors::getDonorCountry, Donors::setDonorCountry);
    }

    // ===================== CRUD =====================
    @Override
    protected void populateForm(Donors entity) throws Exception {
        this.entity = entity;
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected Donors createNewEntity() {
        return new Donors();
    }

    @Override
    protected void focusFirstField() {
        donorLongName.focus();
    }



    // ===================== Grid =====================
    @Override
    protected List<ColumnDef<Donors>> getColumnDefs() {
        return List.of(
                col("id", "ID", Donors::getId, e -> e.getId() + ""),

                col("donorLongName", "Long Name",
                        Donors::getDonorLongName, Donors::getDonorLongName),

                col("donorShortName", "Short Name",
                        Donors::getDonorShortName, Donors::getDonorShortName),

                col("donorCountry", "Country",
                        Donors::getDonorCountry, Donors::getDonorCountry),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                		Donors::getCreatedAt,
                        e -> e.getCreatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                		Donors::getUpdatedAt,
                        e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== Filtering =====================
    @Override
    protected Specification<Donors> buildCombinedSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String quick = quickSearchField.getValue();
            if (quick != null && !quick.isBlank()) {
                String like = "%" + quick.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("donorLongName")), like),
                        cb.like(cb.lower(root.get("donorShortName")), like),
                        cb.like(cb.lower(root.get("donorCountry")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
                new AdvancedSearchPanel.FilterDef(
                        "donorLongName",
                        "Long Name",
                        () -> new TextField(),
                        c -> ((TextField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "donorShortName",
                        "Short Name",
                        () -> new TextField(),
                        c -> ((TextField) c).clear()
                )
        ));
        return advPanel;
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }

    // ===================== Password confirm =====================
    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        return action == SensitiveAction.DELETE;
    }

    @Override
    protected void confirmPassword(SensitiveAction action, Runnable onSuccess) {

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Confirm Password");

        PasswordField pwd = new PasswordField("Password");

        Button confirm = new Button("Confirm", e -> {
            User u = currentUserLogin.orElse(null);
            if (u != null && passwordEncoder.matches(pwd.getValue(), u.getHashedPassword())) {
                dlg.close();
                onSuccess.run();
            } else {
                pwd.setInvalid(true);
                pwd.setErrorMessage("Wrong password");
            }
        });

        dlg.add(new VerticalLayout(pwd));
        dlg.getFooter().add(confirm);
        dlg.open();
    }
}
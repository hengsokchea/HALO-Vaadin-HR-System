package org.halocambodia.views.donors;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.Contracts;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Donors;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.ContractService;
import org.halocambodia.data.DonorsRepository;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

@Route(value = "contracts", layout = MainLayout.class)
@PageTitle("Contracts | កិច្ចសន្យា")
@PermitAll
public class ContractsView extends PageDialogLayout<Contracts, ContractService> {

    private final DonorsRepository donorsRepository;
    private final Optional<User> currentUserLogin;

    // ===================== Fields =====================
    private final TextField contractCode = new TextField("Contract Code | លេខកូដកិច្ចសន្យា");
    private final DatePicker startDate = new DatePicker("Start Date | ថ្ងៃចាប់ផ្តើម");
    private final DatePicker endDate = new DatePicker("End Date | ថ្ងៃបញ្ចប់");
    private final IntegerField amountUSD = new IntegerField("Amount (USD)");
    private final ComboBox<Donors> donor = new ComboBox<>("Donor | អ្នកឧបត្ថម្ភ");

    public ContractsView(ContractService service,
                         AuthenticatedUser authenticatedUser,
                         DonorsRepository donorsRepository) {
        super(Contracts.class, service, null, authenticatedUser);
        this.donorsRepository = donorsRepository;
        this.currentUserLogin = authenticatedUser.get();
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {
        donor.setItems(donorsRepository.findAll());
        donor.setItemLabelGenerator(d -> d == null ? "" : d.getDonorShortName());

        contractCode.setWidthFull();
        donor.setWidthFull();
        startDate.setWidthFull();
        endDate.setWidthFull();
        amountUSD.setWidthFull();
    }

    // ===================== FORM =====================
    @Override
    protected void configureEditorLayout() {

        FormLayout form = new FormLayout(
                contractCode,
                donor,
                startDate,
                endDate,
                amountUSD
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== BINDER =====================
    @Override
    protected void binderField() {

        binder.forField(contractCode)
                .asRequired("Required")
                .bind(Contracts::getContractCode, Contracts::setContractCode);

        binder.forField(donor)
                .asRequired("Donor required")
                .bind(Contracts::getDonor, Contracts::setDonor);

        binder.forField(startDate)
                .bind(Contracts::getStartDate, Contracts::setStartDate);

        binder.forField(endDate)
                .bind(Contracts::getEndDate, Contracts::setEndDate);

        binder.forField(amountUSD)
                .bind(Contracts::getAmountUSD, Contracts::setAmountUSD);
    }

    // ===================== CRUD =====================
    @Override
    protected void populateForm(Contracts entity) {
        this.entity = entity;
        binder.readBean(entity);
        editorLayout.open();
    }

    @Override
    protected Contracts createNewEntity() {
        return new Contracts();
    }

    @Override
    protected void focusFirstField() {
        contractCode.focus();
    }

    // ===================== GRID =====================
    @Override
    protected List<ColumnDef<Contracts>> getColumnDefs() {
        return List.of(
                col("id", "ID", Contracts::getId, e -> e.getId() + ""),

                col("contractCode", contractCode.getLabel(),
                        Contracts::getContractCode, Contracts::getContractCode),

                col("donor", donor.getLabel(),
                        e -> e.getDonor(),
                        e -> e.getDonor() != null ? e.getDonor().getDonorShortName() : ""),

                col("startDate", startDate.getLabel(),
                        Contracts::getStartDate,
                        e -> e.getStartDate() != null ?
                                DateTimeUtilFormart.DATE_FORMATTER.format(e.getStartDate()) : ""),

                col("endDate", endDate.getLabel(),
                        Contracts::getEndDate,
                        e -> e.getEndDate() != null ?
                                DateTimeUtilFormart.DATE_FORMATTER.format(e.getEndDate()) : ""),

                col("amountUSD", amountUSD.getLabel(),
                        Contracts::getAmountUSD,
                        e -> e.getAmountUSD() != null ? e.getAmountUSD().toString() : ""),

                // ===== AUDIT =====
                col("userCreated.name", "Created By",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At",
                        e -> e.getCreatedAt(),
                        e -> e.getCreatedAt() != null ?
                                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt()) : ""),

                col("userUpdated.name", "Updated By",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At",
                        e -> e.getUpdatedAt(),
                        e -> e.getUpdatedAt() != null ?
                                DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt()) : "")
        );
    }

    // ===================== ADVANCED SEARCH =====================
    @Override
    protected Component buildAdvancedSearchLayout() {

        advPanel = new AdvancedSearchPanel(List.of(

                new FilterDef(
                        "contractCode",
                        "Contract Code",
                        () -> new TextField(),
                        c -> ((TextField) c).clear()
                ),

                new FilterDef(
                        "donor",
                        "Donor",
                        () -> {
                            ComboBox<Donors> cb = new ComboBox<>();
                            cb.setItems(donorsRepository.findAll());
                            cb.setItemLabelGenerator(Donors::getDonorShortName);
                            cb.setClearButtonVisible(true);
                            cb.setWidthFull();
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new FilterDef(
                        "startDate",
                        "Start Date",
                        DatePicker::new,
                        c -> ((DatePicker) c).clear()
                ),

                new FilterDef(
                        "endDate",
                        "End Date",
                        DatePicker::new,
                        c -> ((DatePicker) c).clear()
                )
        ));

        return advPanel;
    }

    // ===================== FILTER LOGIC =====================
    @Override
    protected Specification<Contracts> buildCombinedSpecification() {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            List<String> sqlFilter = new ArrayList<>();

            Join<Contracts, Donors> donorJoin = root.join("donor", JoinType.LEFT);

            // Quick search
            String quick = quickSearchField.getValue();
            if (quick != null && !quick.isBlank()) {
                String like = "%" + quick.toLowerCase() + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("contractCode")), like),
                        cb.like(cb.lower(donorJoin.get("donorShortName")), like)
                ));

                sqlFilter.add("Quick: " + quick);
            }

            if (advPanel != null) {

                TextField code = advPanel.getField("contractCode", TextField.class);
                if (code != null && !code.isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("contractCode")),
                            "%" + code.getValue().toLowerCase() + "%"));
                    sqlFilter.add("Code = " + code.getValue());
                }

                ComboBox<Donors> donorField = advPanel.getField("donor", ComboBox.class);
                if (donorField != null && donorField.getValue() != null) {
                    predicates.add(cb.equal(root.get("donor"), donorField.getValue()));
                    sqlFilter.add("Donor = " + donorField.getValue().getDonorShortName());
                }

                DatePicker start = advPanel.getField("startDate", DatePicker.class);
                if (start != null && start.getValue() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), start.getValue()));
                    sqlFilter.add("Start >= " + start.getValue());
                }

                DatePicker end = advPanel.getField("endDate", DatePicker.class);
                if (end != null && end.getValue() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), end.getValue()));
                    sqlFilter.add("End <= " + end.getValue());
                }
            }

            showSqlFilterTokens(sqlFilter);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }
}
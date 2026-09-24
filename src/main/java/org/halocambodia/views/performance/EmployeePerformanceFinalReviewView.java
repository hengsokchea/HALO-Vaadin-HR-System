package org.halocambodia.views.performance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.*;
import org.halocambodia.data.enums.LeaveDuration;
import org.halocambodia.enums.ReviewValueType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeePerformanceFinalReviewService;
import org.halocambodia.services.EmployeePerformanceReviewService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;
import org.springframework.data.jpa.domain.Specification;


import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Route(value = "performance-final-review", layout = MainLayout.class)
@PageTitle("Performance Final Review")
@PermitAll
public class EmployeePerformanceFinalReviewView extends PageDialogLayout<EmployeePerformanceReview, EmployeePerformanceFinalReviewService> {

    // ===================== Header fields =====================
    private ComboBox<Employee> employee;
    private ComboBox<Employee> supervisor;

    private final ComboBox<Branch> branch = new ComboBox<>("Location | តំបន់");
    
    private final ComboBox<Positions> empPosition = new ComboBox<>("Position | មុខតំណែង");
    
    private final ComboBox<Employee> finalReviewBy = new ComboBox<>("Final Approver | អ្នកអនុម័តចុងក្រោយ");
    
    private final DatePicker reviewDate = new DatePicker("Review date | កាលបរិច្ឆេទវាយតម្លៃ");
    private final BigDecimalField overallRating = new BigDecimalField("Overall Rating | ពិន្ទុវាយតម្លៃសរុប");
    private final TextArea managerComments = new TextArea("Line Manager comments | មតិយោបល់");
    
    private final ComboBox<PerformanceStatus> supervisorStatus = new ComboBox<>("Line Manager status | ស្ថានភាពអ្នកគ្រប់គ្រង");
    private final DatePicker supervisorDate = new DatePicker("Line Manager Aprrove date | កាលបរិច្ឆេទអ្នកគ្រប់គ្រង");

    private final ComboBox<PerformanceStatus> finalStatus = new ComboBox<>("Line Manager status | ស្ថានភាពអ្នកគ្រប់គ្រង");
    private final DatePicker finalReviewDate = new DatePicker("Line Manager Aprrove date | កាលបរិច្ឆេទអ្នកគ្រប់គ្រង");
    private final TextArea finalReviewComments = new TextArea("Final Review comments | មតិយោបល់");

    // ===================== Criteria UI state =====================
    private final Map<Long, RadioButtonGroup<Integer>> ratingGroups = new LinkedHashMap<>();
    private final Map<Long, RadioButtonGroup<Boolean>> booleanGroups = new LinkedHashMap<>();
    private final Map<Long, TextArea> textAreas = new LinkedHashMap<>();

    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final PerformanceStatusRepository performanceStatusRepository;

    private Div criteriaContainer; // dynamic body
    
    private final Optional<User> currentUserLogin;

    public EmployeePerformanceFinalReviewView(EmployeePerformanceFinalReviewService service, UserService userService,AuthenticatedUser authenticatedUser, PositionRepository positionRepository,EmployeeRepository employeeRepository,BranchRepository branchRepository,PerformanceStatusRepository performanceStatusRepository) {
        super(EmployeePerformanceReview.class, service, userService, authenticatedUser);
        this.positionRepository = positionRepository;
        this.employeeRepository=employeeRepository;
        this.currentUserLogin = authenticatedUser.get();
        this.branchRepository=branchRepository;
        this.performanceStatusRepository=performanceStatusRepository;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        // ✅ IMPORTANT ORDER:
        // 1) preload creates employee/supervisor ComboBoxes
        // 2) configureEditorLayout uses them
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
        miAddNew.setVisible(false);
        MenuItem miFinalReview=leftMenu.addAction(VaadinIcon.CHECK,null, this::processFinalReview,"Final Reveiw");
    }
    private void processFinalReview() {

        Set<EmployeePerformanceReview> selected = grid.asMultiSelect().getSelectedItems();
        if (selected.isEmpty()) {
            Notification.show("Please select at least 1 row | សូមជ្រើសយ៉ាងហោចណាស់ ១ ជួរ",
                    2500, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            return;
        }

        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader("Confirm Final Review | បញ្ជាក់ការពិនិត្យចុងក្រោយ");
        dlg.setText("Apply final review to " + selected.size() + " row(s)?\n\nអនុវត្តការពិនិត្យចុងក្រោយលើ " + selected.size() + " ជួរ?");
        dlg.setConfirmText("Apply | អនុវត្ត");
        dlg.setConfirmButtonTheme("primary success");
        dlg.setCancelText("Cancel | បោះបង់");
        dlg.setCancelable(true);

        dlg.addConfirmListener(ev -> {
            try {
                Employee me = employeeRepository
                        .findByInsuranceNo(Integer.valueOf(currentUserLogin.get().getInsurance()))
                        .orElse(null);

                PerformanceStatus approved = performanceStatusRepository
                        .findByStatusNameIgnoreCase("APPROVED")
                        .orElse(null);

                for (EmployeePerformanceReview r : selected) {
                    r.setFinalReviewBy(me);
                    r.setFinalReviewDate(LocalDate.now());
                    r.setFinalStatus(approved);

                    currentUserLogin.ifPresent(u -> {
                        r.setUserUpdated(u);
                        if (r.getUserCreated() == null) r.setUserCreated(u);
                    });

                    service.update(r);
                }

                refreshGrid();
                Notification.show("Final review applied ✅", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Failed: " + ex.getMessage(), 3500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });

        // ✅ MUST be here
        dlg.open();
    }

	 
    private void preload() {
    	this.branch.setItems(this.branchRepository.findByIsActiveTrue());
    	this.branch.setItemLabelGenerator(Branch::getBranchShortName);
        employee = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()+ "-"+ e.getInsuranceNo()),
                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                filter -> service.countEmployeesByNameEnKhInsurance(filter)
        );
        employee.setLabel("Employee | បុគ្គលិក");
        employee.setClearButtonVisible(true);
        employee.setHelperText("Employee (Being Reviewed) → បុគ្គលិក (ត្រូវបានវាយតម្លៃ)");
        employee.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;

            Positions pos = (e.getValue() == null) ? null : e.getValue().getPositions();

            empPosition.setValue(pos);
        });


        supervisor = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()) + "-"+ e.getInsuranceNo(),
                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                filter -> service.countEmployeesByNameEnKhInsurance(filter)
        );
        supervisor.setLabel("Line Manager | អ្នកគ្រប់គ្រងផ្ទាល់");
        supervisor.setClearButtonVisible(true);
        supervisor.setReadOnly(true);
        
        supervisor.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;

            Branch br = (e.getValue() == null) ? null : e.getValue().getBranch();

            this.branch.setValue(br);
        });

        empPosition.setItems(positionRepository.findAll(org.springframework.data.domain.Sort.by("position")));
        empPosition.setItemLabelGenerator(p -> formatEnKh(p.getPosition(), p.getPositionKh()));
        empPosition.setClearButtonVisible(true);

        overallRating.setReadOnly(true);
        overallRating.clear();


        managerComments.setWidthFull();
        managerComments.setMinHeight("120px");
        
        finalReviewComments.setWidthFull();
        finalReviewComments.setMinHeight("120px");
        
        branch.addValueChangeListener(e -> refreshFinalReviewBy(e.getValue()));
        
        supervisorStatus.setItems( performanceStatusRepository.findByStatusNameIn(List.of("DRAFT", "SUBMITTED")));
        supervisorStatus.setItemLabelGenerator(PerformanceStatus::getStatusName); 
        
     // initial state
        supervisorDate.setVisible(false);
        supervisorDate.setRequiredIndicatorVisible(false);

        supervisorStatus.addValueChangeListener(ev -> {
            updateSupervisorApprovalUI(ev.getValue());
            binder.validate(); // refresh validation message immediately
        });
        
        finalStatus.setItems( performanceStatusRepository.findByStatusNameIn(List.of("PENDING", "APPROVED","RETURNED")));
        finalStatus.setItemLabelGenerator(PerformanceStatus::getStatusName); 
        
        finalStatus.addValueChangeListener(ev -> {
            updateFinalApprovalUI(ev.getValue());
            binder.validate(); // refresh validation message immediately
        });

        grid.setItemDetailsRenderer(this.createTabRenderer());
        
        this.supervisorStatus.setReadOnly(true);
        this.supervisorDate.setReadOnly(true);

    }
    

    
    private void refreshFinalReviewBy(Branch br) {
        if (br == null) {
            finalReviewBy.clear();
            finalReviewBy.setItems(Collections.emptyList());
            return;
        }

        List<Employee> locationManagers = Optional.ofNullable(br.getBranchManagerAssignments())
                .orElse(Collections.emptyList())
                .stream()
                .map(BranchManagerAssignment::getEmployee)   // adjust if your class name differs
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        finalReviewBy.setItems(locationManagers);
        finalReviewBy.setItemLabelGenerator(ee ->
                formatEnKh(ee.getNameEn(), ee.getNameKh()) + "-" + ee.getInsuranceNo());

        // ✅ If current value is not in list -> clear it
        if (finalReviewBy.getValue() != null && !locationManagers.contains(finalReviewBy.getValue())) {
            finalReviewBy.clear();
        }

        // ✅ Auto-select first record (only when empty)
        if (finalReviewBy.getValue() == null && !locationManagers.isEmpty()) {
            finalReviewBy.setValue(locationManagers.get(0));
        }
    }

    public static String formatEnKh(String en, String kh) {
        boolean enBlank = (en == null || en.trim().isEmpty());
        boolean khBlank = (kh == null || kh.trim().isEmpty());
        if (enBlank && khBlank) return "";
        if (khBlank) return en.trim();
        if (enBlank) return kh.trim();
        return en.trim() + " | " + kh.trim();
    }

    // ===================== Layout =====================

    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout header = new FormLayout( reviewDate,supervisor,branch,employee, empPosition,finalReviewBy);
        header.setWidthFull();
        header.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2),
                new FormLayout.ResponsiveStep("1100px", 3)
        );

        criteriaContainer = new Div();
        criteriaContainer.setWidthFull();
        criteriaContainer.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "0.75rem");

        Div commentsWrap = new Div(managerComments,finalReviewComments);
        commentsWrap.getStyle().set("padding-top", "0.5rem");

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();



        Tab tHeader = new Tab(VaadinIcon.CLIPBOARD_TEXT.create(), new Span("Review Information"));
        Tab tCriteria = new Tab(VaadinIcon.LIST_OL.create(), new Span("Performance criteria"));
        Tab tComments = new Tab(VaadinIcon.COMMENT.create(), new Span("Comments"));
        Tab tApprovals = new Tab(VaadinIcon.CHECK_CIRCLE.create(), new Span("Approvals"));

        for (Tab t : new Tab[]{tHeader, tCriteria, tComments,tApprovals}) {
            t.addThemeVariants(com.vaadin.flow.component.tabs.TabVariant.LUMO_ICON_ON_TOP);
        }

        tabs.add(tHeader, header);
        //tabs.add(tCriteria, criteriaContainer);
        Component criteriaTabContent = buildCriteriaTabContent();
        tabs.add(tCriteria, criteriaTabContent);

        tabs.add(tComments, commentsWrap);
        tabs.add(tApprovals, buildApprovalsTabContent());

        Div wrapper = new Div(tabs);
        wrapper.getStyle().set("padding", "0 1rem").set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }
    private Component buildCriteriaTabContent() {

        overallRating.setReadOnly(true);
        overallRating.setWidth("250px");

        H4 title = new H4("Overall Rating | ពិន្ទុវាយតម្លៃសរុប");
        title.getStyle().set("margin", "0");

        Div summary = new Div(title, overallRating);
        summary.setWidthFull();
        summary.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("gap", "1rem")
                .set("padding", "0.5rem 0.75rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("margin-bottom", "0.75rem");

       Div wrap = new Div(criteriaContainer,summary );
       //wrap.setWidthFull();
       wrap.setWidth("98%");

        return wrap;
    }

    
    private Component buildApprovalsTabContent() {

        supervisorStatus.setWidthFull();
        supervisorDate.setWidthFull();

        FormLayout supForm = new FormLayout(supervisorStatus, supervisorDate);
        supForm.setWidthFull();

        H4 supTitle = new H4("Line Manager approval | អនុម័តដោយអ្នកគ្រប់គ្រង");
        supTitle.getStyle().set("margin", "0 0 0.5rem 0");

        Div supCard = new Div(supTitle, supForm);
        supCard.setWidthFull();
        supCard.getStyle()
                .set("box-sizing", "border-box")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "0.75rem")
                .set("margin-bottom", "0.75rem");
        
        
        FormLayout finalForm = new FormLayout(finalStatus, finalReviewDate);
        finalForm.setWidthFull();
        
        H4 finalTitle = new H4("Final approval | អនុម័តចុងក្រោយ");
        finalTitle.getStyle().set("margin", "0 0 0.5rem 0");
        
        Div finalCard = new Div(finalTitle, finalForm);
        finalCard.setWidthFull();
        finalCard.getStyle()
                .set("box-sizing", "border-box")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "0.75rem")
                .set("margin-bottom", "0.75rem");
        

        return new Div(supCard,finalCard);
    }


    // ===================== CRUD hooks =====================

    @Override
    protected void populateForm(EmployeePerformanceReview entity) throws Exception {
        this.entity = entity;
        boolean isNew = this.entity.getId() == null;
        // ✅ Ensure rating rows exist for all active criteria
        service.ensureRatingRows(entity);

        // ✅ rebuild UI (always from DB so it matches latest criteria)
        rebuildCriteriaUI();

        // ✅ bind header fields
        binder.readBean(entity);

        // ✅ map existing ratings to UI components
        Map<Long, EmployeePerformanceReviewRating> byCriteriaId = (entity.getRatings() == null)
                ? Map.of()
                : entity.getRatings().stream()
                .filter(r -> r.getCriteria() != null && r.getCriteria().getId() != null)
                .collect(Collectors.toMap(r -> r.getCriteria().getId(), r -> r, (a, b) -> a));

        ratingGroups.forEach((criteriaId, rg) -> {
            var r = byCriteriaId.get(criteriaId);
            rg.setValue(r == null ? null : r.getRatingValue());
        });

        booleanGroups.forEach((criteriaId, bg) -> {
            var r = byCriteriaId.get(criteriaId);
            bg.setValue(r == null ? null : r.getBoolValue());
        });

        textAreas.forEach((criteriaId, ta) -> {
            var r = byCriteriaId.get(criteriaId);
            ta.setValue(r == null || r.getTextValue() == null ? "" : r.getTextValue());
        });

        editorLayout.open();
        
        overallRating.setValue(entity.getOverallRating() == null ? BigDecimal.ZERO : entity.getOverallRating());
        if(isNew) {
            refreshFinalReviewBy(entity.getBranch());
        }
        
        updateSupervisorApprovalUI(entity.getSupervisorStatus());
        recalcOverallRating();


    }

    @Override
    protected void beforeSave(EmployeePerformanceReview entity, boolean isNew) throws Exception {
        //service.ensureRatingRows(entity);
        syncRatings(entity);
    }


    @Override
    protected void focusFirstField() {
        employee.focus();
    }

    @Override
    protected EmployeePerformanceReview createNewEntity() {
        EmployeePerformanceReview r = new EmployeePerformanceReview();
        r.setReviewDate(LocalDate.now());
        //r.setOverallRating(3);

        Employee sup = employeeRepository.findByInsuranceNo(Integer.valueOf(currentUserLogin.get().getInsurance())).orElse(null);

        r.setSupervisor(sup);
        Branch br = (sup == null) ? null : sup.getBranch();
        r.setBranch(br);
        r.setSupervisorStatus(performanceStatusRepository.findByStatusNameIgnoreCase("SUBMITTED").orElseThrow());
        r.setFinalStatus(performanceStatusRepository.findByStatusNameIgnoreCase("PENDING").orElseThrow());

        return r;
    }

    @Override
    protected void binderField() {
        binder.forField(employee)
                .asRequired("Employee is required")
                .bind(EmployeePerformanceReview::getEmployee, EmployeePerformanceReview::setEmployee);

        binder.forField(supervisor)
                .asRequired("Supervisor is required")
                .bind(EmployeePerformanceReview::getSupervisor, EmployeePerformanceReview::setSupervisor);
        
        binder.forField(this.branch)
        .asRequired("Location is required")
        .bind(EmployeePerformanceReview::getBranch, EmployeePerformanceReview::setBranch);
        
        binder.forField(this.finalReviewBy)
        .asRequired("Final Reveiw is required")
        .bind(EmployeePerformanceReview::getFinalReviewBy, EmployeePerformanceReview::setFinalReviewBy);

        binder.forField(empPosition)
                .asRequired("Position is required")
                .bind(EmployeePerformanceReview::getPosition, EmployeePerformanceReview::setPosition);

        binder.forField(reviewDate)
                .asRequired("Review date is required")
                .bind(EmployeePerformanceReview::getReviewDate, EmployeePerformanceReview::setReviewDate);
        
        binder.forField(overallRating)
        .asRequired("Overall Rating is required")
        .bind(EmployeePerformanceReview::getOverallRating, EmployeePerformanceReview::setOverallRating);



        binder.bind(managerComments,
                EmployeePerformanceReview::getManagerComments,
                EmployeePerformanceReview::setManagerComments);
        
        binder.bind(finalReviewComments,
                EmployeePerformanceReview::getFinalReviewComments,
                EmployeePerformanceReview::setFinalReviewComments);
        
        binder.forField(supervisorStatus)
        .asRequired("Supervisor is required")
        .bind(EmployeePerformanceReview::getSupervisorStatus, EmployeePerformanceReview::setSupervisorStatus);

	  
	  binder.forField(supervisorDate)
	    .withValidator(date -> !isSubmitted(supervisorStatus.getValue()) || date != null,
	            "Line Manager approve date is required when status is SUBMITTED")
	    .bind(EmployeePerformanceReview::getSupervisorReviewDate,
	          EmployeePerformanceReview::setSupervisorReviewDate);

	  
      binder.forField(finalStatus)
      .asRequired("Final Status is required")
      .bind(EmployeePerformanceReview::getFinalStatus, EmployeePerformanceReview::setFinalStatus);
      
	  binder.forField(finalReviewDate)
	    .withValidator(date -> !isApproved(finalStatus.getValue()) || date != null,
	            "Final approve date is required when status is APPROVED")
	    .bind(EmployeePerformanceReview::getFinalReviewDate,
	          EmployeePerformanceReview::setFinalReviewDate);
	

    }

    // ===================== Dynamic Criteria UI =====================

    private void rebuildCriteriaUI() {
        criteriaContainer.removeAll();
        ratingGroups.clear();
        booleanGroups.clear();
        textAreas.clear();

        // ✅ Load groups+criteria already sorted (service uses repos internally)
        List<EmployeePerformanceFinalReviewService.GroupWithCriteria> gwcs = service.loadActiveCriteria();

        for (var gwc : gwcs) {
            ReviewCriteriaGroup g = gwc.group();
            List<ReviewCriteria> items = gwc.criteria();

            H4 title = new H4(formatEnKh(g.getGroupNameEn(), g.getGroupNameKh()));
            title.getStyle().set("margin", "0.25rem 0 0.75rem 0");

            Div block = new Div();
            block.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "12px")
                    .set("padding", "0.75rem")
                    .set("margin-bottom", "0.75rem");

            block.add(title);
            
            String gDesc = formatEnKh(g.getGroupDescEn(), g.getGroupDescKh());
            if (gDesc != null && !gDesc.isBlank()) {
                Span desc = new Span(gDesc);
                desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
                desc.getStyle().set("font-size", "var(--lumo-font-size-s)");
                desc.getStyle().set("display", "block");
                desc.getStyle().set("margin-bottom", "0.5rem");
                block.add(desc);
            }


            if (items == null || items.isEmpty()) {
                block.add(new Span("No criteria"));
                criteriaContainer.add(block);
                continue;
            }

            for (ReviewCriteria c : items) {
                block.add(buildCriteriaRow(c));
            }

            criteriaContainer.add(block);
        }
    }

    private Component buildCriteriaRow(ReviewCriteria c) {
        String label = formatEnKh(c.getCriteriaNameEn(), c.getCriteriaNameKh());

        Div wrap = new Div();
        wrap.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "1fr")
            .set("gap", "0.5rem")
            .set("padding", "0.5rem 0")
            .set("border-top", "1px dotted var(--lumo-contrast-20pct)")
            .set("min-width", "0") // Important: prevents overflow
            .set("overflow-wrap", "break-word");

        // On larger screens, use 2 columns
        wrap.getStyle().set("--criteria-grid-template", "1fr");
        wrap.getElement().executeJs("""
            const mediaQuery = window.matchMedia('(min-width: 700px)');
            const updateLayout = (e) => {
                this.style.gridTemplateColumns = e.matches ? '1fr auto' : '1fr';
            };
            updateLayout(mediaQuery);
            mediaQuery.addListener(updateLayout);
        """);

        // LEFT: name + description
        Div left = new Div();
        left.getStyle()
            .set("min-width", "0")
            .set("word-break", "break-word");

        Span name = new Span(label);
        name.getStyle().set("font-weight", "600");

        String helper = formatEnKh(c.getCriteriaDescEn(), c.getCriteriaDescKh());

        left.add(name);
        if (helper != null && !helper.isBlank()) {
            Span desc = new Span(helper);
            desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
            desc.getStyle().set("font-size", "var(--lumo-font-size-s)");
            desc.getStyle().set("display", "block");
            desc.getStyle().set("margin-top", "0.15rem");
            left.add(desc);
        }

        Component input;

        if (c.getValueType() == ReviewValueType.RATING) {
            RadioButtonGroup<Integer> rg = new RadioButtonGroup<>();
            rg.setItems(range(c.getMinValue(), c.getMaxValue()));
            rg.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "0.25rem")
                .set("max-width", "100%");
            rg.addValueChangeListener(e -> recalcOverallRating());
            ratingGroups.put(c.getId(), rg);
            input = rg;

        } else if (c.getValueType() == ReviewValueType.BOOLEAN) {
            RadioButtonGroup<Boolean> bg = new RadioButtonGroup<>();
            bg.setItems(Boolean.TRUE, Boolean.FALSE);
            bg.setItemLabelGenerator(v -> Boolean.TRUE.equals(v) ? "Yes" : "No");
            bg.getStyle()
                .set("display", "flex")
                .set("gap", "0.5rem")
                .set("width", "auto");
            bg.addValueChangeListener(e -> recalcOverallRating());
            
            booleanGroups.put(c.getId(), bg);
            input = bg;

        } else {
            TextArea ta = new TextArea();
            ta.setMinHeight("70px");
            ta.getStyle()
                .set("width", "100%")
                .set("max-width", "100%");
            textAreas.put(c.getId(), ta);
            input = ta;
        }

        // RIGHT: input container
        Div right = new Div(input);
        right.getStyle()
            .set("display", "flex")
            .set("justify-content", "flex-end")
            .set("align-items", "center")
            .set("min-width", "0")
            .set("max-width", "100%");

        wrap.add(left, right);
        return wrap;
    }


    private List<Integer> range(Integer min, Integer max) {
        int a = (min == null) ? 1 : min;
        int b = (max == null) ? 5 : max;
        if (b < a) { int t = a; a = b; b = t; } // safety
        List<Integer> out = new ArrayList<>();
        for (int i = a; i <= b; i++) out.add(i);
        return out;
    }

    private void syncRatings(EmployeePerformanceReview review) {

        if (review.getRatings() == null) {
            review.setRatings(new ArrayList<>());
        }

        // ❌ REMOVE THIS (causes new rows on every save)
        // service.ensureRatingRows(review);

        Map<Long, EmployeePerformanceReviewRating> byCriteriaId = new LinkedHashMap<>();
        for (EmployeePerformanceReviewRating r : review.getRatings()) {
            if (r.getCriteria() != null && r.getCriteria().getId() != null) {
                byCriteriaId.putIfAbsent(r.getCriteria().getId(), r);
            }
        }

        Set<Long> activeCids = new LinkedHashSet<>();
        activeCids.addAll(ratingGroups.keySet());
        activeCids.addAll(booleanGroups.keySet());
        activeCids.addAll(textAreas.keySet());

        for (Long cid : activeCids) {
            EmployeePerformanceReviewRating r = byCriteriaId.get(cid);

            // Only create row if really missing in memory
            if (r == null) {
                r = new EmployeePerformanceReviewRating();
                r.setReview(review);
                r.setCriteria(refCriteria(cid));
                review.getRatings().add(r);
                byCriteriaId.put(cid, r);
            }

            r.setRatingValue(null);
            r.setBoolValue(null);
            r.setTextValue(null);

            RadioButtonGroup<Integer> rg = ratingGroups.get(cid);
            if (rg != null) r.setRatingValue(rg.getValue());

            RadioButtonGroup<Boolean> bg = booleanGroups.get(cid);
            if (bg != null) r.setBoolValue(bg.getValue());

            TextArea ta = textAreas.get(cid);
            if (ta != null) {
                String v = ta.getValue();
                r.setTextValue((v == null || v.trim().isEmpty()) ? null : v.trim());
            }
        }
    }



    private ReviewCriteria refCriteria(Long id) {
        ReviewCriteria c = new ReviewCriteria();
        c.setId(id);
        return c;
    }


    // ===================== Grid =====================

    @Override
    protected List<ColumnDef<EmployeePerformanceReview>> getColumnDefs() {
        return List.of(
                col("id", "ID",EmployeePerformanceReview::getId,e -> e.getId() == null ? "" : e.getId().toString()),
                col("branch", this.branch.getLabel(),EmployeePerformanceReview::getBranch, e -> e.getBranch() == null ? "" :e.getBranch().getBranchShortName()),
                col("employee.nameEn", this.employee.getLabel(),EmployeePerformanceReview::getEmployee, e -> e.getEmployee() == null ? "" : formatEnKh(e.getEmployee().getNameEn(), e.getEmployee().getNameKh()+"-"+e.getEmployee().getInsuranceNo().toString())),
                col("position", this.empPosition.getLabel(),EmployeePerformanceReview::getPosition, e -> e.getPosition() == null ? "" : formatEnKh(e.getPosition().getPosition(), e.getPosition().getPositionKh())),
                col("supervisor.nameEn", this.supervisor.getLabel(),EmployeePerformanceReview::getSupervisor, e -> e.getSupervisor() == null ? "" : formatEnKh(e.getSupervisor().getNameEn(), e.getSupervisor().getNameKh())+"-"+e.getEmployee().getInsuranceNo().toString()),
                col("reviewDate", this.reviewDate.getLabel(),EmployeePerformanceReview::getReviewDate, e -> e.getReviewDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format( e.getReviewDate())),
                
                col("supervisorStatus", this.supervisorStatus.getLabel(),EmployeePerformanceReview::getSupervisorStatus, e -> e.getSupervisorStatus() == null ? "" : e.getSupervisorStatus().getStatusName()),
                col("supervisorReviewDate", this.supervisorDate.getLabel(),EmployeePerformanceReview::getSupervisorReviewDate, e -> e.getSupervisorReviewDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format( e.getSupervisorReviewDate())),
                col("managerComments", this.managerComments.getLabel(),EmployeePerformanceReview::getManagerComments, e -> e.getManagerComments() == null ? "" : e.getManagerComments()),
                col("overallRating", this.overallRating.getLabel(),EmployeePerformanceReview::getOverallRating, e -> e.getOverallRating() == null ? "" : e.getOverallRating().toString()),                
                
                
                col("finalReviewBy.nameEn", this.finalReviewBy.getLabel(),EmployeePerformanceReview::getFinalReviewBy, e -> e.getFinalReviewBy() == null ? "" : formatEnKh(e.getFinalReviewBy().getNameEn(), e.getFinalReviewBy().getNameKh())+"-"+e.getFinalReviewBy().getInsuranceNo().toString()),
                col("finalStatus", this.finalStatus.getLabel(),EmployeePerformanceReview::getFinalStatus, e -> e.getFinalStatus() == null ? "" : e.getFinalStatus().getStatusName()),
                col("finalReviewComments", this.finalReviewComments.getLabel(),EmployeePerformanceReview::getFinalReviewComments, e -> e.getFinalReviewComments() == null ? "" : e.getFinalReviewComments()),
                                
                
                col("userCreated.name", "Created By",e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "" ),                
                col("createdAt", "Created At",e->e.getCreatedAt(),e -> e.getCreatedAt() != null  ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt())  : ""),
                col("userUpdated.name", "Updated By",e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),
                col("updatedAt", "Updated At",e->e.getUpdatedAt(), e -> e.getUpdatedAt() != null ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt())  : "")
                
                
        );
    }


    @Override
    protected Specification<EmployeePerformanceReview> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

               // query.distinct(true);

                Join<EmployeePerformanceReview, Employee> employeeJoin = root.join("employee", JoinType.INNER);
                Join<EmployeePerformanceReview, Positions> positionJoin = root.join("position", JoinType.INNER);
                Join<EmployeePerformanceReview, Employee> supervisorJoin = root.join("supervisor", JoinType.INNER);
                Join<EmployeePerformanceReview, PerformanceStatus> supervisorStatusJoin = root.join("supervisorStatus", JoinType.INNER);
                Join<EmployeePerformanceReview, Branch> branchJoin = root.join("branch", JoinType.INNER);
                
                Join<EmployeePerformanceReview, Employee> finalReviewByJoin = root.join("finalReviewBy", JoinType.INNER);
                Join<EmployeePerformanceReview, PerformanceStatus> finalStatusJoin = root.join("finalStatus", JoinType.INNER);
                
                Join<EmployeePerformanceReview, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<EmployeePerformanceReview, User> userUpdated = root.join("userUpdated", JoinType.LEFT);

                //Default Filter
                
                if (currentUserLogin.isPresent() && !currentUserLogin.get().hasRoleId(1L, 2L)) {
                    Integer loginInsuranceNo = Integer.valueOf(currentUserLogin.get().getInsurance());
                    predicates.add(cb.equal(finalReviewByJoin.get("insuranceNo"), loginInsuranceNo));
                }


                
                
                predicates.add(supervisorStatusJoin.get("statusName").in("SUBMITTED"));
                predicates.add(finalStatusJoin.get("statusName").in("PENDING","RETURNED"));
                
                
                
                // Quick search
                String quick = quickSearchField.getValue();
                if (quick != null && !quick.isEmpty()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, branchJoin.get("branchShortName"), like),
                            
                            buildLikePredicate(cb, employeeJoin.get("nameEn"), like),
                            buildLikePredicate(cb, employeeJoin.get("nameKh"), like),
                            buildLikePredicate(cb, employeeJoin.get("insuranceNo"), like),
                            
                            buildLikePredicate(cb, positionJoin.get("position"), like),
                            buildLikePredicate(cb, positionJoin.get("positionKh"), like),
                            
                            buildLikePredicate(cb, supervisorJoin.get("nameEn"), like),
                            buildLikePredicate(cb, supervisorJoin.get("nameKh"), like),
                            buildLikePredicate(cb, supervisorJoin.get("insuranceNo"), like),
                            
                            buildLikePredicate(cb, root.get("reviewDate"), like),
                            buildLikePredicate(cb, supervisorStatusJoin.get("statusName"), like),
                            buildLikePredicate(cb, root.get("supervisorReviewDate"), like),
                            buildLikePredicate(cb, root.get("managerComments"), like),
                            buildLikePredicate(cb, root.get("overallRating"), like),
                            
                            
                            buildLikePredicate(cb, finalReviewByJoin.get("nameEn"), like),
                            buildLikePredicate(cb, finalReviewByJoin.get("nameKh"), like),
                            buildLikePredicate(cb, finalReviewByJoin.get("insuranceNo"), like),
                            buildLikePredicate(cb, finalStatusJoin.get("statusName"), like),
                            
                            buildLikePredicate(cb, userCreated.get("name"), like),
                            buildLikePredicate(cb, userUpdated.get("name"), like),
                            buildLikePredicate(cb, root.get("createdAt"), like),
                            buildLikePredicate(cb, root.get("updatedAt"), like)
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
                    MultiSelectComboBox<Branch> advanceFilterBranchJoin =(MultiSelectComboBox<Branch>) advPanel.getField("branch", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, branchJoin,advanceFilterBranchJoin == null ? null : advanceFilterBranchJoin.getValue(), Branch::getBranchShortName, this.branch.getLabel(), predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Employee> advanceSearchEmployee =(MultiSelectComboBox<Employee>) advPanel.getField("employee", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, employeeJoin,advanceSearchEmployee == null ? null : advanceSearchEmployee.getValue(), e-> formatEnKh( e.getNameEn() , e.getNameKh()) + "-" + e.getInsuranceNo().toString(), this.employee.getLabel(), predicates, sqlFilter);
                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Positions> advanceSearchPosition =(MultiSelectComboBox<Positions>) advPanel.getField("position", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, positionJoin,advanceSearchPosition == null ? null : advanceSearchPosition.getValue(), e-> formatEnKh( e.getPosition() , e.getPositionKh()) , this.empPosition.getLabel(), predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Employee> advanceSearchSupervisor =(MultiSelectComboBox<Employee>) advPanel.getField("supervisor", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, supervisorJoin,advanceSearchSupervisor == null ? null : advanceSearchSupervisor.getValue(), e-> formatEnKh( e.getNameEn() , e.getNameKh()) + "-" + e.getInsuranceNo().toString(), this.supervisor.getLabel(), predicates, sqlFilter);
                                        
                    DateRangePicker advanceFilterReviewDate = advPanel.getField("reviewDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("reviewDate")),advanceFilterReviewDate == null ? null : advanceFilterReviewDate.getFrom(),advanceFilterReviewDate == null ? null : advanceFilterReviewDate.getTo(), this.reviewDate.getLabel(), predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<PerformanceStatus> advanceFilterSupervisorStatus =(MultiSelectComboBox<PerformanceStatus>) advPanel.getField("supervisorStatus", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, supervisorStatusJoin,advanceFilterSupervisorStatus == null ? null : advanceFilterSupervisorStatus.getValue(), PerformanceStatus::getStatusName, this.supervisorStatus.getLabel(), predicates, sqlFilter);

                    DateRangePicker advanceFilterSupervisorReviewDate = advPanel.getField("supervisorReviewDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("supervisorReviewDate")),advanceFilterSupervisorReviewDate == null ? null : advanceFilterSupervisorReviewDate.getFrom(),advanceFilterSupervisorReviewDate == null ? null : advanceFilterSupervisorReviewDate.getTo(), this.supervisorDate.getLabel(), predicates, sqlFilter);
                    
                   TextField AdvanceFilterManagerComments = advPanel.getField("managerComments", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("managerComments"), AdvanceFilterManagerComments == null ? null : AdvanceFilterManagerComments.getValue(),this.managerComments.getLabel(), predicates, sqlFilter);

                    BigDecimalField advanceFilterOverallRating = advPanel.getField("overallRating", BigDecimalField.class);
                    if (advanceFilterOverallRating != null && advanceFilterOverallRating.getValue() != null) {
                        BigDecimal idVal = advanceFilterOverallRating.getValue();
                        predicates.add(cb.equal(root.get("overallRating"), idVal));
                        sqlFilter.add(this.overallRating.getLabel() + " = " + idVal);
                    }
                    
                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<Employee> advanceFilterFinalReviewBy =(MultiSelectComboBox<Employee>) advPanel.getField("finalReviewBy", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, finalReviewByJoin,advanceFilterFinalReviewBy == null ? null : advanceFilterFinalReviewBy.getValue(), e-> formatEnKh( e.getNameEn() , e.getNameKh()) + "-" + e.getInsuranceNo().toString(), this.finalReviewBy.getLabel(), predicates, sqlFilter);
                    
                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<PerformanceStatus> advanceFilterFinalStatus =(MultiSelectComboBox<PerformanceStatus>) advPanel.getField("finalStatus", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, finalStatusJoin,advanceFilterFinalStatus == null ? null : advanceFilterFinalStatus.getValue(), PerformanceStatus::getStatusName, "Final Review Status", predicates, sqlFilter);

                    

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> createdByMs =(MultiSelectComboBox<User>) advPanel.getField("userCreated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userCreated,createdByMs == null ? null : createdByMs.getValue(), User::getName, "CreatedBy", predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<User> updatedByMs = (MultiSelectComboBox<User>) advPanel.getField("userUpdated", MultiSelectComboBox.class);
                    advanceFilterBuildInPredicate(cb, userUpdated, updatedByMs == null ? null : updatedByMs.getValue(), User::getName, "Updated By", predicates, sqlFilter);

                    DateRangePicker createdRange = advPanel.getField("createdAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("createdAt")),createdRange == null ? null : createdRange.getFrom(),createdRange == null ? null : createdRange.getTo(), "CreatedAt", predicates, sqlFilter);

                    DateRangePicker updatedRange = advPanel.getField("updatedAt", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,cb.function("DATE", LocalDate.class, root.get("updatedAt")),updatedRange == null ? null : updatedRange.getFrom(), updatedRange == null ? null : updatedRange.getTo(),"UpdatedAt", predicates, sqlFilter);
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
                            tf.setPlaceholder("Equal");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((IntegerField) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "branch",
                        this.branch.getLabel(),
                        () -> buildMultiSelect(
                        		e->e.getBranchShortName(),
                        		this.branchRepository.findByIsActiveTrue()
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "employee",
                        this.employee.getLabel(),
                        () -> buildLazyMultiSelect(
                        		e -> formatEnKh(e.getNameEn(), e.getNameKh())+"-"+e.getInsuranceNo(),
                                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                                filter -> service.countEmployeesByNameEnKhInsurance(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "position",
                        this.empPosition.getLabel(),
                        () -> buildMultiSelect(
                        		e -> formatEnKh(e.getPosition(), e.getPositionKh()),
                        		this.positionRepository.findAll()
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                
                new AdvancedSearchPanel.FilterDef(
                        "supervisor",
                        this.supervisor.getLabel(),
                        () -> buildLazyMultiSelect(
                        		e -> formatEnKh(e.getNameEn(), e.getNameKh())+"-"+e.getInsuranceNo(),
                                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                                filter -> service.countEmployeesByNameEnKhInsurance(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "reviewDate",
                        this.reviewDate.getLabel(),
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "supervisorStatus",
                        this.supervisorStatus.getLabel(),
                        () -> buildMultiSelect(
                        		e-> e.getStatusName(),
                        		this.performanceStatusRepository.findAllById(List.of(1L,2L))
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "supervisorReviewDate",
                        this.supervisorDate.getLabel(),
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "managerComments",
                        this.managerComments.getLabel(),
                        () -> {
                            TextField tf = new TextField();
                            tf.setPlaceholder("Contains...");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((TextField) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "overallRating",
                        this.overallRating.getLabel(),
                        () -> {
                            BigDecimalField tf = new BigDecimalField();
                            tf.setPlaceholder("Equal");
                            tf.setWidthFull();
                            return tf;
                        },
                        c -> ((BigDecimalField) c).clear()
                ),
                
                
                new AdvancedSearchPanel.FilterDef(
                        "finalReviewBy",
                        this.finalReviewBy.getLabel(),
                        () -> buildLazyMultiSelect(
                        		e -> formatEnKh(e.getNameEn(), e.getNameKh())+"-"+e.getInsuranceNo(),
                                (filter, pageable) -> service.searchEmployeesByNameEnKhInsurance(filter, pageable),
                                filter -> service.countEmployeesByNameEnKhInsurance(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                
                new AdvancedSearchPanel.FilterDef(
                        "finalStatus",
                        "Final Review Status",
                        () -> buildMultiSelect(
                        		e-> e.getStatusName(),
                        		this.performanceStatusRepository.findAllById(List.of(3L,4L,5L))
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
   
                new AdvancedSearchPanel.FilterDef(
                        "userCreated",
                        "Created By",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "createdAt",
                        "Created At",
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "userUpdated",
                        "Updated By",
                        () -> buildLazyMultiSelect(
                                User::getName,
                                (filter, pageable) -> userService.searchUsersByName(filter, pageable),
                                filter -> userService.countUsersByName(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),
                new AdvancedSearchPanel.FilterDef(
                        "updatedAt",
                        "Updated At",
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
    
    private boolean isSubmitted(PerformanceStatus st) {
        return st != null && "SUBMITTED".equalsIgnoreCase(st.getStatusName());
    }

    private void updateSupervisorApprovalUI(PerformanceStatus st) {
        boolean submitted = isSubmitted(st);

        supervisorDate.setVisible(submitted);
        supervisorDate.setRequiredIndicatorVisible(submitted);

        // optional: auto-set date when submitted
        if (submitted && supervisorDate.getValue() == null) {
            supervisorDate.setValue(LocalDate.now());
        }

        // optional: clear date when not submitted
        if (!submitted) {
            supervisorDate.clear();
        }
    }

    
    private boolean isApproved(PerformanceStatus st) {
        return st != null && "APPROVED".equalsIgnoreCase(st.getStatusName());
    }
    private void updateFinalApprovalUI(PerformanceStatus st) {
        boolean submitted = isApproved(st);

        finalReviewDate.setVisible(submitted);
        finalReviewDate.setRequiredIndicatorVisible(submitted);

        // optional: auto-set date when submitted
        if (submitted && finalReviewDate.getValue() == null) {
        	finalReviewDate.setValue(LocalDate.now());
        }

        // optional: clear date when not submitted
        if (!submitted) {
        	finalReviewDate.clear();
        }
    }
    
    private void recalcOverallRating() {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (RadioButtonGroup<Integer> rg : ratingGroups.values()) {
            Integer v = rg.getValue();
            if (v != null) {
                sum = sum.add(BigDecimal.valueOf(v));              
            }
            count++;
        }

        if (count == 0) {
            overallRating.setValue(BigDecimal.ZERO);
            return;
        }

        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP); // ✅ 2 decimals

        overallRating.setValue(avg);

        // optional: also push into entity immediately (so save gets it even if binder not bound)
        if (this.entity != null) {
            this.entity.setOverallRating(avg);
        }
    }

    private ComponentRenderer<Component, EmployeePerformanceReview> createTabRenderer() {
        return new ComponentRenderer<>(review -> {

            TabSheet tabSheet = new TabSheet();
            tabSheet.setSizeFull();

            tabSheet.add("Performance criteria", buildCriteriaReadOnlyView(review));

            VerticalLayout wrapper = new VerticalLayout(tabSheet);
            wrapper.setPadding(false);
            wrapper.setSpacing(false);
            wrapper.setMargin(false);
            wrapper.setSizeFull();
            wrapper.setHeight("600px");
            wrapper.setFlexGrow(1, tabSheet);

            return wrapper;
        });
    }

    private Component buildCriteriaReadOnlyView(EmployeePerformanceReview review) {

        // ✅ map saved ratings by criteriaId
        Map<Long, EmployeePerformanceReviewRating> byCriteriaId =
                Optional.ofNullable(review.getRatings()).orElse(Collections.emptyList())
                        .stream()
                        .filter(r -> r.getCriteria() != null && r.getCriteria().getId() != null)
                        .collect(Collectors.toMap(r -> r.getCriteria().getId(), r -> r, (a, b) -> a, LinkedHashMap::new));

        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "0.75rem");

        // Optional: overall summary
        H4 title = new H4("Overall Rating | ពិន្ទុវាយតម្លៃសរុប");
        title.getStyle().set("margin", "0");

        BigDecimalField overall = new BigDecimalField();
        overall.setReadOnly(true);
        overall.setWidth("250px");
        overall.setValue(review.getOverallRating() == null ? BigDecimal.ZERO : review.getOverallRating());

        Div summary = new Div(title, overall);
        summary.setWidthFull();
        summary.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("gap", "1rem")
                .set("padding", "0.5rem 0.75rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("margin-bottom", "0.75rem");

        // ✅ Load groups+criteria (same as your editable UI)
        List<EmployeePerformanceFinalReviewService.GroupWithCriteria> gwcs = service.loadActiveCriteria();

        for (var gwc : gwcs) {
            ReviewCriteriaGroup g = gwc.group();
            List<ReviewCriteria> items = gwc.criteria();

            H4 groupTitle = new H4(formatEnKh(g.getGroupNameEn(), g.getGroupNameKh()));
            groupTitle.getStyle().set("margin", "0.25rem 0 0.75rem 0");

            Div block = new Div();
            block.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-10pct)")
                    .set("border-radius", "12px")
                    .set("padding", "0.75rem")
                    .set("margin-bottom", "0.75rem");

            block.add(groupTitle);

            String gDesc = formatEnKh(g.getGroupDescEn(), g.getGroupDescKh());
            if (gDesc != null && !gDesc.isBlank()) {
                Span desc = new Span(gDesc);
                desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
                desc.getStyle().set("font-size", "var(--lumo-font-size-s)");
                desc.getStyle().set("display", "block");
                desc.getStyle().set("margin-bottom", "0.5rem");
                block.add(desc);
            }

            if (items == null || items.isEmpty()) {
                block.add(new Span("No criteria"));
                container.add(block);
                continue;
            }

            for (ReviewCriteria c : items) {
                EmployeePerformanceReviewRating saved = byCriteriaId.get(c.getId());
                block.add(buildCriteriaRowReadOnly(c, saved));
            }

            container.add(block);
        }

        Div wrap = new Div(container, summary);
        wrap.setWidth("98%");
        return wrap;
    }

    private Component buildCriteriaRowReadOnly(ReviewCriteria c, EmployeePerformanceReviewRating saved) {

        String label = formatEnKh(c.getCriteriaNameEn(), c.getCriteriaNameKh());

        Div wrap = new Div();
        wrap.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr")
                .set("gap", "0.5rem")
                .set("padding", "0.5rem 0")
                .set("border-top", "1px dotted var(--lumo-contrast-20pct)")
                .set("min-width", "0")
                .set("overflow-wrap", "break-word");

        wrap.getElement().executeJs("""
            const mq = window.matchMedia('(min-width: 700px)');
            const update = (e) => { this.style.gridTemplateColumns = e.matches ? '1fr auto' : '1fr'; };
            update(mq);
            mq.addListener(update);
        """);

        Div left = new Div();
        left.getStyle().set("min-width", "0").set("word-break", "break-word");

        Span name = new Span(label);
        name.getStyle().set("font-weight", "600");

        String helper = formatEnKh(c.getCriteriaDescEn(), c.getCriteriaDescKh());
        left.add(name);
        if (helper != null && !helper.isBlank()) {
            Span desc = new Span(helper);
            desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
            desc.getStyle().set("font-size", "var(--lumo-font-size-s)");
            desc.getStyle().set("display", "block");
            desc.getStyle().set("margin-top", "0.15rem");
            left.add(desc);
        }

        Component input;

        if (c.getValueType() == ReviewValueType.RATING) {
            RadioButtonGroup<Integer> rg = new RadioButtonGroup<>();
            rg.setItems(range(c.getMinValue(), c.getMaxValue()));
            rg.setValue(saved == null ? null : saved.getRatingValue());
            rg.setReadOnly(true);
            rg.setEnabled(false); // ✅ no change
            rg.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "0.25rem");
            input = rg;

        } else if (c.getValueType() == ReviewValueType.BOOLEAN) {
            RadioButtonGroup<Boolean> bg = new RadioButtonGroup<>();
            bg.setItems(Boolean.TRUE, Boolean.FALSE);
            bg.setItemLabelGenerator(v -> Boolean.TRUE.equals(v) ? "Yes" : "No");
            bg.setValue(saved == null ? null : saved.getBoolValue());
            bg.setReadOnly(true);
            bg.setEnabled(false);
            bg.getStyle().set("display", "flex").set("gap", "0.75rem");
            input = bg;

        } else {
            TextArea ta = new TextArea();
            ta.setValue(saved == null || saved.getTextValue() == null ? "" : saved.getTextValue());
            ta.setReadOnly(true);
            ta.setWidthFull();
            ta.setMinHeight("70px");
            input = ta;
        }

        Div right = new Div(input);
        right.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center")
                .set("min-width", "0")
                .set("max-width", "100%");

        wrap.add(left, right);
        return wrap;
    }

}

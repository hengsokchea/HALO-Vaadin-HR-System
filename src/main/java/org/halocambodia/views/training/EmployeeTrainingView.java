package org.halocambodia.views.training;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.DateRangePicker;
import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeTraining;
import org.halocambodia.data.TrainingProvider;
import org.halocambodia.data.TrainingCourse;
import org.halocambodia.data.User;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.TrainingCenterRepository;
import org.halocambodia.data.TrainingCourseRepository;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.EmployeeTrainingService;
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
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
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

@Route(value = "employee-trainings", layout = MainLayout.class)
@PageTitle("Employee Training | បណ្ដុះបណ្ដាលបុគ្គលិក")
@PermitAll
public class EmployeeTrainingView extends PageDialogLayout<EmployeeTraining, EmployeeTrainingService> {

    private final Optional<User> currentUserLogin;
    private final PasswordEncoder passwordEncoder;

    private final EmployeeRepository employeeRepository;
    private final TrainingCourseRepository trainingCourseRepository;
    private final TrainingCenterRepository trainingCenterRepository;

    // ===================== Editor fields =====================
    private ComboBox<Employee> employee;

    private final ComboBox<TrainingCourse> trainingType =
            new ComboBox<>("Training Course | វគ្គបណ្ដុះបណ្ដាល");

    private final ComboBox<TrainingProvider> trainingCenter =
            new ComboBox<>("Training Provider | អ្នកផ្តល់ការបណ្តុះបណ្តាល");

    private final DatePicker startDate = new DatePicker("Start Date | ថ្ងៃចាប់ផ្តើម");
    private final DatePicker endDate   = new DatePicker("End Date | ថ្ងៃបញ្ចប់");

    // ✅ NEW
    private final BigDecimalField maxScore = new BigDecimalField("Max Score | ពិន្ទុអតិបរមា");

    private final BigDecimalField score = new BigDecimalField("Score | ពិន្ទុ");

    private final ComboBox<String> result = new ComboBox<>("Result | លទ្ធផល");

    private final TextField trainer = new TextField("Trainer | គ្រូបង្ហាត់");
    private final TextArea remark  = new TextArea("Remark | កំណត់សម្គាល់");

    private final BigDecimalField haloSupportAmount =
            new BigDecimalField("HALO Support Amount (USD) | ថវិកាជំនួយ HALO (USD)");

    protected AdvancedSearchPanel advPanel;

    public EmployeeTrainingView(
            EmployeeTrainingService service,
            UserService userService,
            AuthenticatedUser authenticatedUser,
            PasswordEncoder passwordEncoder,
            EmployeeRepository employeeRepository,
            TrainingCourseRepository trainingCourseRepository,
            TrainingCenterRepository trainingCenterRepository
    ) {
        super(EmployeeTraining.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
        this.passwordEncoder = passwordEncoder;

        this.employeeRepository = employeeRepository;
        this.trainingCourseRepository = trainingCourseRepository;
        this.trainingCenterRepository = trainingCenterRepository;
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        preload();
        configureGrid();
        configureEditorLayout();
        binderField();
    }

    private void preload() {

        trainingType.setWidthFull();
        trainingType.setClearButtonVisible(true);
        trainingType.setPlaceholder("Select course... | ជ្រើសរើសវគ្គ...");
        trainingType.setItemLabelGenerator(tc -> tc == null ? "" : nvl(tc.getTrainingCourseName()));

        trainingCenter.setWidthFull();
        trainingCenter.setClearButtonVisible(true);
        trainingCenter.setPlaceholder("Select provider... | ជ្រើសរើសអ្នកផ្តល់...");
        trainingCenter.setItemLabelGenerator(this::formatTrainingCenterLabel);

        this.employee = buildLazyComboBox(
                e -> formatEnKh(e.getNameEn(), e.getNameKh()) + "-" + e.getInsuranceNo(),
                (filter, pageable) -> employeeRepository.searchEmployeesByNameEnKhInsuranceList(filter, pageable),
                filter -> employeeRepository.countEmployeesByNameEnKhInsuranceTrimmed(filter)
        );
        employee.setLabel("Employee | បុគ្គលិក");

        trainingType.setItems(trainingCourseRepository.findByIsActiveTrueOrderByTrainingCourseNameAsc());
        trainingCenter.setItems(trainingCenterRepository.findAll(Sort.by(Sort.Direction.ASC, "trainingProviderName")));

        startDate.setWidthFull();
        endDate.setWidthFull();

        // ✅ NEW maxScore config
        maxScore.setWidthFull();
        maxScore.setPlaceholder("0.00");
        maxScore.setClearButtonVisible(true);


        score.setWidthFull();
        score.setPlaceholder("0.00");
        score.setClearButtonVisible(true);


        result.setWidthFull();
        result.setClearButtonVisible(true);
        result.setPlaceholder("Select... | ជ្រើសរើស...");
        result.setItems("Pass", "Failed");

        trainer.setWidthFull();
        trainer.setClearButtonVisible(true);

        haloSupportAmount.setWidthFull();
        haloSupportAmount.setPlaceholder("0.00");
        haloSupportAmount.setClearButtonVisible(true);


        remark.setWidthFull();
        remark.setMinHeight("120px");
        remark.setPlaceholder("Notes... | កំណត់សម្គាល់...");
    }

    // ===================== Layout =====================
    @Override
    protected void configureEditorLayout() throws Exception {

        FormLayout form = new FormLayout(
                employee, trainingType,
                trainingCenter, haloSupportAmount,
                startDate, endDate,
                maxScore, score,
                result, trainer,
                remark
        );

        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("800px", 2)
        );

        form.setColspan(remark, 2);

        editorLayout.add(new VerticalLayout(form));
        configureEditorFooter();
    }

    // ===================== CRUD hooks =====================
    @Override
    protected void populateForm(EmployeeTraining entity) throws Exception {
        this.entity = entity;
        binder.readBean(entity);

        if (entity.getHaloSupportAmount() == null) {
            haloSupportAmount.setValue(BigDecimal.ZERO);
        }
        // optional defaults
        if (entity.getMaxScore() == null) {
            maxScore.clear();
        }

        editorLayout.open();
    }

    @Override
    protected EmployeeTraining createNewEntity() throws Exception {
        EmployeeTraining t = new EmployeeTraining();
        t.setHaloSupportAmount(BigDecimal.ZERO);
        return t;
    }

    @Override
    protected void focusFirstField() {
        employee.focus();
    }

    @Override
    protected void binderField() {

        binder.forField(employee)
                .asRequired("Employee is required | ត្រូវការបុគ្គលិក")
                .bind(EmployeeTraining::getEmployee, EmployeeTraining::setEmployee);

        binder.forField(trainingType)
                .asRequired("Training Course is required | ត្រូវការវគ្គបណ្ដុះបណ្ដាល")
                .bind(EmployeeTraining::getTrainingType, EmployeeTraining::setTrainingType);

        binder.forField(trainingCenter)
                .asRequired("Training Provider is required | ត្រូវការអ្នកផ្តល់ការបណ្តុះបណ្តាល")
                .bind(EmployeeTraining::getTrainingCenter, EmployeeTraining::setTrainingCenter);

        binder.bind(startDate, EmployeeTraining::getStartDate, EmployeeTraining::setStartDate);

        binder.forField(endDate)
                .asRequired("End Date is required | ត្រូវការថ្ងៃបញ្ចប់")
                .bind(EmployeeTraining::getEndDate, EmployeeTraining::setEndDate);

        // ✅ NEW
        binder.bind(maxScore, EmployeeTraining::getMaxScore, EmployeeTraining::setMaxScore);

        // ✅ score with validator: score <= maxScore (if maxScore provided)
        binder.forField(score)
                .withValidator(v -> {
                    if (v == null) return true;
                    BigDecimal ms = maxScore.getValue();
                    return ms == null || v.compareTo(ms) <= 0;
                }, "Score must be <= Max Score | ពិន្ទុត្រូវតែតូចជាង ឬស្មើ ពិន្ទុអតិបរមា")
                .bind(EmployeeTraining::getScore, EmployeeTraining::setScore);

        binder.bind(result, EmployeeTraining::getResult, EmployeeTraining::setResult);
        binder.bind(trainer, EmployeeTraining::getTrainer, EmployeeTraining::setTrainer);
        binder.bind(remark, EmployeeTraining::getRemark, EmployeeTraining::setRemark);

        binder.forField(haloSupportAmount)
                .asRequired("HALO Support Amount is required | ត្រូវការថវិកាជំនួយ HALO")
                .bind(EmployeeTraining::getHaloSupportAmount, EmployeeTraining::setHaloSupportAmount);
    }

    // ===================== Grid columns =====================
    @Override
    protected List<ColumnDef<EmployeeTraining>> getColumnDefs() {
        return List.of(
                col("id", "ID",
                        EmployeeTraining::getId,
                        e -> e.getId() == null ? "" : e.getId().toString()),

                col("employee.nameEn", employee.getLabel(),
                        e -> e.getEmployee() == null ? null : e.getEmployee().getNameEn(),
                        e -> formatEmployeeLabel(e.getEmployee())),

                col("trainingType.trainingCourseName", trainingType.getLabel(),
                        e -> e.getTrainingType() == null ? null : e.getTrainingType().getTrainingCourseName(),
                        e -> e.getTrainingType() == null ? "" : nvl(e.getTrainingType().getTrainingCourseName())),

                col("trainingCenter.trainingProviderName", trainingCenter.getLabel(),
                        e -> e.getTrainingCenter() == null ? null : e.getTrainingCenter().getTrainingProviderName(),
                        e -> formatTrainingCenterLabel(e.getTrainingCenter())),

                col("startDate", startDate.getLabel(),
                        EmployeeTraining::getStartDate,
                        e -> e.getStartDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(e.getStartDate())),

                col("endDate", endDate.getLabel(),
                        EmployeeTraining::getEndDate,
                        e -> e.getEndDate() == null ? "" : DateTimeUtilFormart.DATE_FORMATTER.format(e.getEndDate())),

                // ✅ NEW column
                col("maxScore", maxScore.getLabel(),
                        EmployeeTraining::getMaxScore,
                        e -> e.getMaxScore() == null ? "" : e.getMaxScore().toString()),

                col("score", score.getLabel(),
                        EmployeeTraining::getScore,
                        e -> e.getScore() == null ? "" : e.getScore().toString()),

                col("result", result.getLabel(),
                        EmployeeTraining::getResult,
                        e -> nvl(e.getResult())),

                col("trainer", trainer.getLabel(),
                        EmployeeTraining::getTrainer,
                        e -> nvl(e.getTrainer())),

                col("haloSupportAmount", haloSupportAmount.getLabel(),
                        EmployeeTraining::getHaloSupportAmount,
                        e -> e.getHaloSupportAmount() == null ? "" : e.getHaloSupportAmount().toString()),

                col("remark", remark.getLabel(),
                        EmployeeTraining::getRemark,
                        e -> nvl(e.getRemark())),

                col("userCreated.name", "Created By | បង្កើតដោយ",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : "",
                        e -> e.getUserCreated() != null ? e.getUserCreated().getName() : ""),

                col("createdAt", "Created At | កាលបរិច្ឆេទបង្កើត",
                        EmployeeTraining::getCreatedAt,
                        e -> e.getCreatedAt() != null
                                ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getCreatedAt())
                                : ""),

                col("userUpdated.name", "Updated By | កែប្រែដោយ",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : "",
                        e -> e.getUserUpdated() != null ? e.getUserUpdated().getName() : ""),

                col("updatedAt", "Updated At | កាលបរិច្ឆេទកែប្រែ",
                        EmployeeTraining::getUpdatedAt,
                        e -> e.getUpdatedAt() != null
                                ? DateTimeUtilFormart.DATE_TIME_FORMATTER.format(e.getUpdatedAt())
                                : "")
        );
    }

    // ===================== Filtering =====================
    @Override
    protected Specification<EmployeeTraining> buildCombinedSpecification() {
        return (root, query, cb) -> {
            try {
                List<Predicate> predicates = new ArrayList<>();
                List<String> sqlFilter = new ArrayList<>();

                Join<EmployeeTraining, Employee> empJ = root.join("employee", JoinType.LEFT);
                Join<EmployeeTraining, TrainingCourse> courseJ = root.join("trainingType", JoinType.LEFT);
                Join<EmployeeTraining, TrainingProvider> centerJ = root.join("trainingCenter", JoinType.LEFT);

                Join<EmployeeTraining, User> userCreated = root.join("userCreated", JoinType.LEFT);
                Join<EmployeeTraining, User> userUpdated = root.join("userUpdated", JoinType.LEFT);

                // Quick search
                String quick = quickSearchField == null ? null : quickSearchField.getValue();
                if (quick != null && !quick.isBlank()) {
                    String like = "%" + quick.toLowerCase().trim() + "%";
                    predicates.add(cb.or(
                            buildLikePredicate(cb, root.get("id"), like),
                            buildLikePredicate(cb, cb.lower(empJ.get("nameEn")), like),
                            buildLikePredicate(cb, cb.lower(courseJ.get("trainingCourseName")), like),
                            buildLikePredicate(cb, cb.lower(centerJ.get("trainingProviderName")), like),
                            buildLikePredicate(cb, cb.lower(root.get("result")), like),
                            buildLikePredicate(cb, cb.lower(root.get("trainer")), like),
                            buildLikePredicate(cb, cb.lower(root.get("remark")), like),
                            buildLikePredicate(cb, cb.lower(userCreated.get("name")), like),
                            buildLikePredicate(cb, cb.lower(userUpdated.get("name")), like),
                            // ✅ include score + maxScore in quick search (optional but helpful)
                            buildLikePredicate(cb, root.get("score"), like),
                            buildLikePredicate(cb, root.get("maxScore"), like)
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
                    MultiSelectComboBox<Employee> advanceSearchEmployee =
                            (MultiSelectComboBox<Employee>) advPanel.getField("employee", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(
                            cb,
                            empJ,
                            advanceSearchEmployee == null ? null : advanceSearchEmployee.getValue(),
                            e -> formatEnKh(e.getNameEn(), e.getNameKh()) + "-" + (e.getInsuranceNo() == null ? "" : e.getInsuranceNo().toString()),
                            this.employee.getLabel(),
                            predicates,
                            sqlFilter
                    );

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<TrainingCourse> courseMs =
                            (MultiSelectComboBox<TrainingCourse>) advPanel.getField("trainingType", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(cb, courseJ,
                            courseMs == null ? null : courseMs.getValue(),
                            tc -> tc == null ? "" : nvl(tc.getTrainingCourseName()),
                            trainingType.getLabel(), predicates, sqlFilter);

                    @SuppressWarnings("unchecked")
                    MultiSelectComboBox<TrainingProvider> centerMs =
                            (MultiSelectComboBox<TrainingProvider>) advPanel.getField("trainingCenter", MultiSelectComboBox.class);

                    advanceFilterBuildInPredicate(cb, centerJ,
                            centerMs == null ? null : centerMs.getValue(),
                            this::formatTrainingCenterLabel,
                            trainingCenter.getLabel(), predicates, sqlFilter);

                    // ✅ NEW: maxScore exact match
                    BigDecimalField maxScoreField = advPanel.getField("maxScore", BigDecimalField.class);
                    if (maxScoreField != null && maxScoreField.getValue() != null) {
                        predicates.add(cb.equal(root.get("maxScore"), maxScoreField.getValue()));
                        sqlFilter.add(maxScore.getLabel() + " = " + maxScoreField.getValue());
                    }

                    @SuppressWarnings("unchecked")
                    ComboBox<String> resultCb = (ComboBox<String>) advPanel.getField("result", ComboBox.class);
                    if (resultCb != null && resultCb.getValue() != null && !resultCb.getValue().isBlank()) {
                        predicates.add(cb.equal(root.get("result"), resultCb.getValue()));
                        sqlFilter.add(result.getLabel() + " = '" + resultCb.getValue().replace("'", "''") + "'");
                    }

                    TextField trainerField = advPanel.getField("trainer", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("trainer"),
                            trainerField == null ? null : trainerField.getValue(),
                            trainer.getLabel(), predicates, sqlFilter);

                    TextField remarkField = advPanel.getField("remark", TextField.class);
                    advanceFilterBuildLikePredicate(cb, root.get("remark"),
                            remarkField == null ? null : remarkField.getValue(),
                            remark.getLabel(), predicates, sqlFilter);

                    DateRangePicker startRange = advPanel.getField("startDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,
                            root.get("startDate"),
                            startRange == null ? null : startRange.getFrom(),
                            startRange == null ? null : startRange.getTo(),
                            "Start Date", predicates, sqlFilter);

                    DateRangePicker endRange = advPanel.getField("endDate", DateRangePicker.class);
                    advanceFilterBuildBetweenPredicate(cb,
                            root.get("endDate"),
                            endRange == null ? null : endRange.getFrom(),
                            endRange == null ? null : endRange.getTo(),
                            "End Date", predicates, sqlFilter);

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

        List<TrainingCourse> courses = trainingCourseRepository.findAll(Sort.by(Sort.Direction.ASC, "trainingCourseName"));
        List<TrainingProvider> centers = trainingCenterRepository.findAll(Sort.by(Sort.Direction.ASC, "trainingProviderName"));

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
                        "employee",
                        this.employee.getLabel(),
                        () -> buildLazyMultiSelect(
                                e -> formatEnKh(e.getNameEn(), e.getNameKh()) + "-" + (e.getInsuranceNo() == null ? "" : e.getInsuranceNo().toString()),
                                (filter, pageable) -> this.employeeRepository.searchEmployeesByNameEnKhInsuranceList(filter, pageable),
                                filter -> employeeRepository.countEmployeesByNameEnKhInsuranceTrimmed(filter)
                        ),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "trainingType",
                        trainingType.getLabel(),
                        () -> buildMultiSelect(tc -> tc == null ? "" : nvl(tc.getTrainingCourseName()), courses),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "trainingCenter",
                        trainingCenter.getLabel(),
                        () -> buildMultiSelect(this::formatTrainingCenterLabel, centers),
                        c -> ((MultiSelectComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "startDate",
                        startDate.getLabel(),
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "endDate",
                        endDate.getLabel(),
                        () -> {
                            DateRangePicker dr = new DateRangePicker();
                            dr.setWidthFull();
                            return dr;
                        },
                        c -> ((DateRangePicker) c).clear()
                ),

                // ✅ NEW: maxScore
                new AdvancedSearchPanel.FilterDef(
                        "maxScore",
                        maxScore.getLabel(),
                        () -> {
                            BigDecimalField bf = new BigDecimalField();
                            bf.setPlaceholder("Equal | ស្មើ");
                            bf.setWidthFull();
                            bf.setClearButtonVisible(true);

                            return bf;
                        },
                        c -> ((BigDecimalField) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "result",
                        result.getLabel(),
                        () -> {
                            ComboBox<String> cb = new ComboBox<>();
                            cb.setWidthFull();
                            cb.setClearButtonVisible(true);
                            cb.setPlaceholder("Select... | ជ្រើសរើស...");
                            cb.setItems("Pass", "Failed");
                            return cb;
                        },
                        c -> ((ComboBox<?>) c).clear()
                ),

                new AdvancedSearchPanel.FilterDef(
                        "trainer",
                        trainer.getLabel(),
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

    // ===================== Password confirm for delete =====================
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



    private String formatTrainingCenterLabel(TrainingProvider c) {
        if (c == null) return "";
        return nvl(c.getTrainingProviderName());
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}

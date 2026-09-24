package org.halocambodia.views.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.persistence.criteria.Predicate;

import org.halocambodia.component.AdvancedSearchPanel;
import org.halocambodia.component.AdvancedSearchPanel.FilterDef;
import org.halocambodia.data.PolicyCategory;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.services.PolicyCategoryService;
import org.halocambodia.services.UserService;
import org.halocambodia.views.MainLayout;
import org.halocambodia.views.PageDialogLayout;

@Route(value = "policy-categories", layout = MainLayout.class)
@PageTitle("Policy Categories")
@PermitAll
@Uses(Icon.class)
public class PolicyCategoryView extends PageDialogLayout<PolicyCategory, PolicyCategoryService> {

    private final TextField code = new TextField("Code | កូដ");
    private final TextField nameEn = new TextField("Name (English) | ឈ្មោះ (អង់គ្លេស)");
    private final TextField nameKh = new TextField("Name (Khmer) | ឈ្មោះ (ខ្មែរ)");
    private final TextArea description = new TextArea("Description | ការពណ៌នា");
    private final DatePicker obsoleteDate = new DatePicker("Obsolete Date | ថ្ងៃឈប់ប្រើ");

    private final Optional<User> currentUserLogin;

    public PolicyCategoryView(PolicyCategoryService service, 
                              UserService userService, 
                              AuthenticatedUser authenticatedUser) {
        super(PolicyCategory.class, service, userService, authenticatedUser);
        this.currentUserLogin = authenticatedUser.get();
    }

    @Override
    protected void onViewAttachOnce(AttachEvent attachEvent) throws Exception {
        this.enableToggleColumn = false;
        this.toggleColumnFrozen = false;

        configureGrid();
        configureEditorLayout();
        binderField();
    }

    @Override
    protected void configureEditorLayout() throws Exception {
        java.util.function.Function<Component[], FormLayout> form = (components) -> {
            FormLayout fl = new FormLayout(components);
            fl.setWidthFull();
            fl.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3),
                new FormLayout.ResponsiveStep("1300px", 4)
            );
            for (Component c : components) {
                if (c instanceof com.vaadin.flow.component.HasSize hs) {
                    hs.setWidthFull();
                }
            }
            return fl;
        };

        code.setRequired(true);
        code.setRequiredIndicatorVisible(true);
        nameEn.setRequired(true);
        nameEn.setRequiredIndicatorVisible(true);
        nameKh.setRequired(true);
        nameKh.setRequiredIndicatorVisible(true);
        description.setHeight("100px");
        obsoleteDate.setPlaceholder("Leave empty for active | ទុកចោលសម្រាប់សកម្ម");

        FormLayout basicForm = form.apply(new Component[] {
            code, nameEn, nameKh, description, obsoleteDate
        });

        basicForm.setColspan(description, 4);
        basicForm.setColspan(obsoleteDate, 4);

        Div wrapper = new Div(basicForm);
        wrapper.getStyle().set("padding", "0 1rem").set("box-sizing", "border-box");

        editorLayout.add(wrapper);
        configureEditorFooter();
    }

    @Override
    protected void populateForm(PolicyCategory entityValue) throws Exception {
        if (entityValue != null && entityValue.getId() != null) {
            this.entity = service.findById(entityValue.getId())
                .orElseThrow(() -> new IllegalArgumentException("This record no longer exists. Please refresh the page."));
        } else {
            this.entity = entityValue;
        }

        binder.readBean(this.entity);
        editorLayout.open();
    }

    @Override
    protected void beforeSave(PolicyCategory entity, boolean isNew) throws Exception {
        // Any validation logic before save
        if (entity.getCode() == null || entity.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Code is required | កូដត្រូវបានទាមទារ");
        }
        if (entity.getNameEn() == null || entity.getNameEn().trim().isEmpty()) {
            throw new IllegalArgumentException("English name is required | ឈ្មោះអង់គ្លេសត្រូវបានទាមទារ");
        }
        if (entity.getNameKh() == null || entity.getNameKh().trim().isEmpty()) {
            throw new IllegalArgumentException("Khmer name is required | ឈ្មោះខ្មែរត្រូវបានទាមទារ");
        }
    }

    @Override
    protected void binderField() {
        binder.bindInstanceFields(this);
    }

    @Override
    protected void focusFirstField() {
        this.code.focus();
    }

    @Override
    protected PolicyCategory createNewEntity() {
        PolicyCategory entity = new PolicyCategory();
        return entity;
    }

    @Override
    protected List<ColumnDef<PolicyCategory>> getColumnDefs() {
        List<ColumnDef<PolicyCategory>> cols = new ArrayList<>(List.of(
            col("id", "ID", PolicyCategory::getId, e -> e.getId() != null ? e.getId().toString() : ""),
            col("code", this.code.getLabel(), PolicyCategory::getCode, e -> e.getCode() != null ? e.getCode() : ""),
            col("nameEn", this.nameEn.getLabel(), PolicyCategory::getNameEn, e -> e.getNameEn() != null ? e.getNameEn() : ""),
            col("nameKh", this.nameKh.getLabel(), PolicyCategory::getNameKh, e -> e.getNameKh() != null ? e.getNameKh() : ""),
            col("description", this.description.getLabel(), PolicyCategory::getDescription, e -> e.getDescription() != null ? e.getDescription() : ""),
            col("obsoleteDate", this.obsoleteDate.getLabel(), 
                PolicyCategory::getObsoleteDate, 
                e -> e.getObsoleteDate() != null ? e.getObsoleteDate().toString() : "Active | សកម្ម")
        ));
        //cols.addAll(gridMetaData());
        return cols;
    }

    @Override
    protected Component buildAdvancedSearchLayout() {
        advPanel = new AdvancedSearchPanel(List.of(
            new FilterDef(
                "code",
                this.code.getLabel(),
                () -> {
                    TextField tf = new TextField();
                    tf.setPlaceholder("Contains...");
                    tf.setWidthFull();
                    return tf;
                },
                c -> ((TextField) c).clear()
            ),
            new FilterDef(
                "nameEn",
                this.nameEn.getLabel(),
                () -> {
                    TextField tf = new TextField();
                    tf.setPlaceholder("Contains...");
                    tf.setWidthFull();
                    return tf;
                },
                c -> ((TextField) c).clear()
            ),
            new FilterDef(
                "nameKh",
                this.nameKh.getLabel(),
                () -> {
                    TextField tf = new TextField();
                    tf.setPlaceholder("Contains...");
                    tf.setWidthFull();
                    return tf;
                },
                c -> ((TextField) c).clear()
            )
        ));

        return advPanel;
    }

    @Override
    protected Specification<PolicyCategory> buildCombinedSpecification() {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            List<String> sqlFilter = new ArrayList<>();

            String quick = quickSearchField.getValue();
            if (quick != null && !quick.isEmpty()) {
                String like = "%" + quick.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                    buildLikePredicate(cb, root.get("code"), like),
                    buildLikePredicate(cb, root.get("nameEn"), like),
                    buildLikePredicate(cb, root.get("nameKh"), like)
                ));
            }

            // Advanced filter
            if (advPanel != null) {
                TextField advCode = advPanel.getField("code", TextField.class);
                advanceFilterBuildLikePredicate(
                    cb, root.get("code"),
                    advCode == null ? null : advCode.getValue(),
                    this.code.getLabel(),
                    predicates, sqlFilter
                );

                TextField advNameEn = advPanel.getField("nameEn", TextField.class);
                advanceFilterBuildLikePredicate(
                    cb, root.get("nameEn"),
                    advNameEn == null ? null : advNameEn.getValue(),
                    this.nameEn.getLabel(),
                    predicates, sqlFilter
                );

                TextField advNameKh = advPanel.getField("nameKh", TextField.class);
                advanceFilterBuildLikePredicate(
                    cb, root.get("nameKh"),
                    advNameKh == null ? null : advNameKh.getValue(),
                    this.nameKh.getLabel(),
                    predicates, sqlFilter
                );
            }

            showSqlFilterTokens(sqlFilter);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    protected void resetAdvancedSearchFields() {
        if (advPanel != null) advPanel.clearAll();
    }

    @Override
    protected Set<String> getAdditionalExcludedKeys() {
        return Set.of("policies");
    }

    @Override
    protected boolean requiresPasswordConfirmation(SensitiveAction action) {
        if (action == SensitiveAction.DELETE) {
            return true;
        } else {
            return false;
        }
    }
}
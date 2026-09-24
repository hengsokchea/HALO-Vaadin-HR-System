package org.halocambodia.views.gazetteer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Gazetteer;
import org.halocambodia.data.User;
import org.halocambodia.services.GazetteerFilter;
import org.halocambodia.services.GazetteerService;
import org.halocambodia.views.MainLayout;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "gazetteer", layout = MainLayout.class)
@PageTitle("Gazetteer")
@PermitAll
@Uses(Icon.class)
public class GazetteerView extends VerticalLayout {

    // ---- Levels (only 4) ----
    private static final int LEVEL_PROVINCE = 1;
    private static final int LEVEL_DISTRICT = 2;
    private static final int LEVEL_COMMUNE  = 3;
    private static final int LEVEL_VILLAGE  = 4;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GazetteerService service;

    private final TreeGrid<Gazetteer> grid = new TreeGrid<>();

    private final TextField search = new TextField();
    private final ComboBox<Integer> levelFilter = new ComboBox<>("Level");

    private final Button btnAddRoot = new Button("Add Province (Root)");
    private final Button btnAddChild = new Button("Add Child");
    private final Button btnEdit = new Button("Edit");
    private final Button btnDelete = new Button("Delete");
    private final Button btnRefresh = new Button("Refresh");

    private Gazetteer selected;

    public GazetteerView(GazetteerService service) {
        this.service = service;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H3("Gazetteer"));
        add(buildToolbar());

        configureGrid();
        add(grid);

        setFlexGrow(1, grid);

        refresh();
    }

    private HorizontalLayout buildToolbar() {
        search.setPlaceholder("Search code / EN / KH...");
        search.setClearButtonVisible(true);
        search.setWidth("320px");
        search.addKeyDownListener(Key.ENTER, e -> refresh());
        search.addValueChangeListener(e -> {
            if (e.isFromClient() && (e.getValue() == null || e.getValue().trim().isEmpty())) {
                refresh();
            }
        });

        levelFilter.setItems(LEVEL_PROVINCE, LEVEL_DISTRICT, LEVEL_COMMUNE, LEVEL_VILLAGE);
        levelFilter.setItemLabelGenerator(GazetteerView::levelName);
        levelFilter.setClearButtonVisible(true);
        levelFilter.addValueChangeListener(e -> refresh());

        btnAddRoot.addClickListener(e -> {
            Gazetteer g = new Gazetteer();
            g.setLevel(LEVEL_PROVINCE); // fixed
            openEditor(g, null);
        });

        btnAddChild.addClickListener(e -> {
            if (selected == null) return;

            Integer pl = selected.getLevel();
            if (pl == null || pl >= LEVEL_VILLAGE) return;

            Gazetteer child = new Gazetteer();
            child.setParent(selected);
            child.setLevel(pl + 1); // fixed
            openEditor(child, selected);
        });

        btnEdit.addClickListener(e -> {
            if (selected == null) return;
            openEditor(selected, selected.getParent());
        });

        btnDelete.addClickListener(e -> {
            if (selected == null) return;
            confirmDelete(selected);
        });

        btnRefresh.addClickListener(e -> refresh());

        btnAddChild.setEnabled(false);
        btnEdit.setEnabled(false);
        btnDelete.setEnabled(false);

        HorizontalLayout bar = new HorizontalLayout(
                search, levelFilter, btnAddRoot, btnAddChild, btnEdit, btnDelete, btnRefresh
        );
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(Alignment.END);
        bar.expand(search);
        return bar;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addHierarchyColumn(g -> safe(g.getNameEn()))
                .setHeader("Name (EN)")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(2);

        grid.addColumn(g -> safe(g.getNameKh()))
                .setHeader("Name (KH)")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(2);

        grid.addColumn(g -> safe(g.getCode()))
                .setHeader("Code")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(1);

        grid.addColumn(g -> levelName(g.getLevel()))
                .setHeader("Level")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(0);

        // ✅ NOTE
        grid.addColumn(g -> safe(g.getNote()))
                .setHeader("Note")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(2);

        // ✅ Created/Updated by + dates (from AbstractEntity)
        grid.addColumn(g -> userLabel(g.getUserCreated()))
                .setHeader("Created By")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(0);

        grid.addColumn(g -> formatZdt(g.getCreatedAt()))
                .setHeader("Created At")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(0);

        grid.addColumn(g -> userLabel(g.getUserUpdated()))
                .setHeader("Updated By")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(0);

        grid.addColumn(g -> formatZdt(g.getUpdatedAt()))
                .setHeader("Updated At")
                .setAutoWidth(true).setResizable(true)
                .setFlexGrow(0);

        grid.setDataProvider(new GazetteerHierarchicalProvider());

        grid.asSingleSelect().addValueChangeListener(e -> {
            selected = e.getValue();
            boolean has = selected != null;

            btnAddChild.setEnabled(has && selected.getLevel() != null && selected.getLevel() < LEVEL_VILLAGE);
            btnEdit.setEnabled(has);
            btnDelete.setEnabled(has);
        });
    }

    private void refresh() {
        grid.getDataProvider().refreshAll();
    }

    private void openEditor(Gazetteer bean, Gazetteer parentForNew) {
        Dialog dlg = new Dialog();
        dlg.setWidth("720px");

        Binder<Gazetteer> binder = new BeanValidationBinder<>(Gazetteer.class);

        TextField code = new TextField("Code");
        TextField nameEn = new TextField("Name (EN)");
        TextField nameKh = new TextField("Name (KH)");

        TextArea note = new TextArea("Note");
        note.setWidthFull();
        note.setMinHeight("120px");

        // ✅ Level read-only ALWAYS
        ComboBox<Integer> level = new ComboBox<>("Level");
        level.setItems(LEVEL_PROVINCE, LEVEL_DISTRICT, LEVEL_COMMUNE, LEVEL_VILLAGE);
        level.setItemLabelGenerator(GazetteerView::levelName);
        level.setClearButtonVisible(false);
        level.setReadOnly(true);

        ComboBox<Gazetteer> parent = new ComboBox<>("Parent");
        parent.setItemLabelGenerator(g -> safe(g.getNameEn()) + " (" + safe(g.getCode()) + ")");
        parent.setClearButtonVisible(true);

        // bind fields (DO NOT bind parent)
        binder.forField(code).asRequired().bind(Gazetteer::getCode, Gazetteer::setCode);
        binder.forField(nameEn).asRequired().bind(Gazetteer::getNameEn, Gazetteer::setNameEn);
        binder.forField(nameKh).asRequired().bind(Gazetteer::getNameKh, Gazetteer::setNameKh);
        binder.forField(level).asRequired().bind(Gazetteer::getLevel, Gazetteer::setLevel);
        binder.bind(note, Gazetteer::getNote, Gazetteer::setNote);

        // init items to avoid "ComboBox without items" error
        parent.setItems(List.of());

        if (bean.getId() == null) {
            // NEW
            if (parentForNew == null) {
                // NEW ROOT
                if (bean.getLevel() == null) bean.setLevel(LEVEL_PROVINCE);
                parent.clear();
                parent.setItems(List.of());
                parent.setReadOnly(true);
            } else {
                // NEW CHILD
                Integer pl = parentForNew.getLevel();
                if (pl != null && pl < LEVEL_VILLAGE) {
                    bean.setLevel(pl + 1);
                }

                Integer lv = bean.getLevel();
                if (lv != null && lv > LEVEL_PROVINCE) {
                    List<Gazetteer> items = service.listPossibleParents(lv);
                    parent.setItems(items);

                    Gazetteer match = items.stream()
                            .filter(x -> x.getId() != null && x.getId().equals(parentForNew.getId()))
                            .findFirst()
                            .orElse(null);

                    parent.setValue(match);
                    parent.setReadOnly(true);
                } else {
                    parent.clear();
                    parent.setItems(List.of());
                    parent.setReadOnly(true);
                }
            }
        } else {
            // EDIT
            Integer lv = bean.getLevel();
            Gazetteer currentParent = bean.getParent();

            if (lv != null && lv > LEVEL_PROVINCE) {
                List<Gazetteer> items = service.listPossibleParents(lv);
                parent.setItems(items);
                parent.setReadOnly(false);

                if (currentParent != null && currentParent.getId() != null) {
                    Gazetteer match = items.stream()
                            .filter(x -> x.getId() != null && x.getId().equals(currentParent.getId()))
                            .findFirst()
                            .orElse(null);
                    parent.setValue(match);
                }
            } else {
                parent.clear();
                parent.setItems(List.of());
                parent.setReadOnly(true);
            }
        }

        binder.setBean(bean);

        FormLayout form = new FormLayout(code, nameEn, nameKh, level, parent, note);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("560px", 2)
        );
        form.setColspan(code, 2);
        form.setColspan(note, 2);

        Button save = new Button("Save", ev -> {
            if (!binder.validate().isOk()) return;

            Gazetteer toSave = binder.getBean();

            // set parent manually
            if (toSave.getLevel() != null && toSave.getLevel() == LEVEL_PROVINCE) {
                toSave.setParent(null);
            } else {
                toSave.setParent(parent.getValue());
            }

            // validate hierarchy
            if (toSave.getLevel() != null && toSave.getLevel() > LEVEL_PROVINCE) {
                if (toSave.getParent() == null) {
                    parent.setInvalid(true);
                    parent.setErrorMessage("Parent is required for " + levelName(toSave.getLevel()));
                    return;
                }
                Integer pl = toSave.getParent().getLevel();
                if (pl == null || pl != toSave.getLevel() - 1) {
                    parent.setInvalid(true);
                    parent.setErrorMessage("Parent must be " + levelName(toSave.getLevel() - 1));
                    return;
                }
            }

            service.save(toSave);
            dlg.close();
            refresh();
        });

        Button cancel = new Button("Cancel", ev -> dlg.close());
        HorizontalLayout actions = new HorizontalLayout(save, cancel);

        dlg.add(form, actions);
        dlg.open();
    }

    private void confirmDelete(Gazetteer g) {
        boolean hasChildren = g.getId() != null && service.hasChildren(g.getId());

        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Delete Gazetteer?");
        cd.setText(hasChildren
                ? "This item has children. Delete is blocked. Remove children first."
                : "Are you sure you want to delete: " + safe(g.getNameEn()) + " ?");
        cd.setCancelable(true);

        if (hasChildren) {
            cd.setConfirmText("OK");
            cd.addConfirmListener(e -> {});
        } else {
            cd.setConfirmText("Delete");
            cd.addConfirmListener(e -> {
                service.delete(g);
                selected = null;
                refresh();
            });
        }
        cd.open();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String formatZdt(ZonedDateTime zdt) {
        return zdt == null ? "" : DateTimeUtilFormart.DATE_TIME_FORMATTER.format(zdt);
    }

    // ✅ CHANGE THIS based on your User entity fields
    private static String userLabel(User u) {
        if (u == null) return "";
        // Examples (choose ONE that exists):
        // return safe(u.getNameEn());
        // return safe(u.getUsername());
        // return safe(u.getEmail());
        return u.getName().toString(); // <-- replace this with real field
    }

    private static String levelName(Integer lv) {
        if (lv == null) return "";
        return switch (lv) {
            case LEVEL_PROVINCE -> "1 - Province";
            case LEVEL_DISTRICT -> "2 - District";
            case LEVEL_COMMUNE  -> "3 - Commune";
            case LEVEL_VILLAGE  -> "4 - Village";
            default -> String.valueOf(lv);
        };
    }

    // -------------------- Hierarchical Data Provider --------------------
    private class GazetteerHierarchicalProvider
            extends AbstractBackEndHierarchicalDataProvider<Gazetteer, GazetteerFilter> {

        @Override
        public int getChildCount(HierarchicalQuery<Gazetteer, GazetteerFilter> query) {
            Gazetteer parent = query.getParent();
            GazetteerFilter filter = currentFilter();
            long count = service.countChildren(parent, filter);
            return (int) Math.min(count, Integer.MAX_VALUE);
        }

        @Override
        public boolean hasChildren(Gazetteer item) {
            if (item == null || item.getId() == null) return false;
            return service.hasChildren(item.getId());
        }

        @Override
        protected java.util.stream.Stream<Gazetteer> fetchChildrenFromBackEnd(
                HierarchicalQuery<Gazetteer, GazetteerFilter> query) {

            Gazetteer parent = query.getParent();
            GazetteerFilter filter = currentFilter();

            int offset = query.getOffset();
            int limit = query.getLimit();

            var sortOrders = query.getSortOrders();
            var sort = (sortOrders == null || sortOrders.isEmpty())
                    ? org.springframework.data.domain.Sort.by("nameEn").ascending()
                    : org.springframework.data.domain.Sort.by("nameEn").ascending();

            List<Gazetteer> rows = service.fetchChildren(parent, filter, offset, limit, sort);
            return rows.stream();
        }

        private GazetteerFilter currentFilter() {
            return new GazetteerFilter(search.getValue(), levelFilter.getValue());
        }
    }
}

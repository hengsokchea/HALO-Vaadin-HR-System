package org.halocambodia.views.payroll;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

/**
 * Reusable National Staff-style grid: bilingual heading, quick search,
 * add/edit/delete/refresh toolbar, striped rows and resizable columns.
 */
public class PayrollAdminGridSection<T> extends VerticalLayout {

    private final Grid<T> grid;
    private final TextField search = new TextField();
    private final Button add = new Button("New | ថ្មី", VaadinIcon.PLUS_CIRCLE.create());
    private final Button edit = new Button("Edit | កែប្រែ", VaadinIcon.EDIT.create());
    private final Button delete = new Button("Delete | លុប", VaadinIcon.TRASH.create());
    private final Button refresh = new Button("Refresh | ផ្ទុកឡើងវិញ", VaadinIcon.REFRESH.create());
    private Function<String, List<T>> loader = value -> List.of();
    private Runnable addAction = () -> { };
    private Consumer<T> editAction = value -> { };
    private Consumer<T> deleteAction = value -> { };

    public PayrollAdminGridSection(String title, String subtitle, Class<T> beanType, boolean editable) {
        this.grid = new Grid<>(beanType, false);
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        H3 heading = new H3(title);
        heading.getStyle().set("margin", "0").set("color", "var(--lumo-primary-text-color)");
        Paragraph help = new Paragraph(subtitle);
        help.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        search.setPlaceholder("Quick Search | ស្វែងរករហ័ស");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidth("320px");
        search.addValueChangeListener(event -> refresh());

        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClickListener(event -> addAction.run());
        edit.addClickListener(event -> selected().ifPresent(editAction));
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        delete.addClickListener(event -> selected().ifPresent(deleteAction));
        refresh.addClickListener(event -> refresh());

        edit.setEnabled(false);
        delete.setEnabled(false);
        add.setVisible(editable);
        edit.setVisible(editable);
        delete.setVisible(editable);

        HorizontalLayout toolbar = new HorizontalLayout(add, edit, delete, refresh, search);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setWrap(true);
        toolbar.setPadding(true);
        toolbar.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");

        grid.setWidthFull();
        grid.setHeight("420px");
        grid.addThemeVariants(
                GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_ROW_STRIPES,
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addSelectionListener(event -> {
            boolean selected = event.getFirstSelectedItem().isPresent();
            edit.setEnabled(editable && selected);
            delete.setEnabled(editable && selected);
        });
        add(heading, help, toolbar, grid);
    }

    public Grid<T> grid() {
        return grid;
    }

    public TextField searchField() {
        return search;
    }

    public PayrollAdminGridSection<T> withLoader(Function<String, List<T>> loader) {
        this.loader = loader;
        return this;
    }

    public PayrollAdminGridSection<T> withActions(
            Runnable addAction, Consumer<T> editAction, Consumer<T> deleteAction) {
        this.addAction = addAction;
        this.editAction = editAction;
        this.deleteAction = deleteAction;
        return this;
    }

    public void refresh() {
        grid.setItems(loader.apply(search.getValue()));
    }

    public void clearSelection() {
        grid.deselectAll();
    }

    public java.util.Optional<T> selected() {
        return grid.getSelectedItems().stream().findFirst();
    }

    public void setGridHeight(String height) {
        grid.setAllRowsVisible(false);
        grid.setHeight(height);
    }

    /**
     * Lets short detail grids grow to the exact number of loaded rows instead of
     * using an internal vertical scrollbar.
     */
    public void setAllRowsVisible(boolean allRowsVisible) {
        if (allRowsVisible) {
            grid.getStyle().remove("height");
        }
        grid.setAllRowsVisible(allRowsVisible);
    }

}

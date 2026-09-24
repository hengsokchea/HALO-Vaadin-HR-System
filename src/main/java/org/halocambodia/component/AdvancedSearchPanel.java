package org.halocambodia.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AdvancedSearchPanel extends VerticalLayout {

	public static record FilterDef(
	        String key,
	        String label,
	        Supplier<? extends Component> factory,
	        Consumer<? super Component> clear
	) {}



    private static class ActiveFilter {
        final FilterDef def;
        final Component field;
        final Component row;

        ActiveFilter(FilterDef def, Component field, Component row) {
            this.def = def;
            this.field = field;
            this.row = row;
        }
    }

    private final Map<String, FilterDef> defs = new LinkedHashMap<>();
    private final Map<String, ActiveFilter> active = new LinkedHashMap<>();

    private final ComboBox<FilterDef> cbAdd = new ComboBox<>();
    private final VerticalLayout body = new VerticalLayout();

    public AdvancedSearchPanel(List<FilterDef> filterDefs) {
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        filterDefs.forEach(d -> defs.put(d.key(), d));

        cbAdd.setPlaceholder("Add filter...");
        cbAdd.setClearButtonVisible(true);
        cbAdd.setItemLabelGenerator(FilterDef::label);
        cbAdd.setWidthFull();
        cbAdd.setAllowCustomValue(false);

        Button btnAdd = new Button(VaadinIcon.PLUS.create(), e -> addSelected());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);

        HorizontalLayout top = new HorizontalLayout(cbAdd, btnAdd);
        top.setWidthFull();
        top.setAlignItems(FlexComponent.Alignment.CENTER);
        top.setFlexGrow(1, cbAdd);

        body.setPadding(false);
        body.setSpacing(false);
        body.setWidthFull();

        add(top, body);

        refreshChoices();
    }

    private void addSelected() {
        FilterDef def = cbAdd.getValue();
        if (def == null) return;
        addFilter(def.key());
        cbAdd.clear();
    }

    public void addFilter(String key) {
        if (key == null || key.isBlank() || active.containsKey(key)) return;

        FilterDef def = defs.get(key);
        if (def == null) return;

        Component field = def.factory().get();
       

        Span lbl = new Span(def.label());
        lbl.getStyle().set("min-width", "160px").set("font-weight", "600");

        Button remove = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> removeFilter(def.key()));
        remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);

        HorizontalLayout row = new HorizontalLayout(lbl, field, remove);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, field);
        row.getStyle().set("padding", "var(--lumo-space-xs) 0");

        body.add(row);
        active.put(def.key(), new ActiveFilter(def, field, row));

        refreshChoices();
    }

    public void removeFilter(String key) {
        ActiveFilter af = active.remove(key);
        if (af == null) return;

        try {
            af.def.clear().accept(af.field);
        } catch (Exception ignore) {}

        body.remove(af.row);
        refreshChoices();
    }

    public boolean isActive(String key) {
        return active.containsKey(key);
    }

    public <C extends Component> C getField(String key, Class<C> type) {
        ActiveFilter af = active.get(key);
        if (af == null) return null;
        return type.isInstance(af.field) ? type.cast(af.field) : null;
    }

    public Set<String> getActiveKeys() {
        return new LinkedHashSet<>(active.keySet());
    }

    public void clearAll() {
        new ArrayList<>(active.keySet()).forEach(this::removeFilter);
    }

    private void refreshChoices() {
        List<FilterDef> available = defs.values().stream()
                .filter(d -> !active.containsKey(d.key()))
                .collect(Collectors.toList());
        cbAdd.setItems(available);
    }
}

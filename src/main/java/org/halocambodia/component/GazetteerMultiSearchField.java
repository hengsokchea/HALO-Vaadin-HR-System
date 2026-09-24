package org.halocambodia.component;

import java.util.*;
import java.util.stream.Collectors;

import org.halocambodia.data.Gazetteer;
import org.halocambodia.services.GazetteerService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;

public class GazetteerMultiSearchField extends CustomField<Void> {

    public enum LayoutMode { RESPONSIVE, VERTICAL, HORIZONTAL }

    private final GazetteerService service;

    private final MultiSelectComboBox<Gazetteer> provinces = new MultiSelectComboBox<>("Province | ខេត្ត");
    private final MultiSelectComboBox<Gazetteer> districts = new MultiSelectComboBox<>("District | ស្រុក");
    private final MultiSelectComboBox<Gazetteer> communes  = new MultiSelectComboBox<>("Commune | ឃុំ");
    private final MultiSelectComboBox<Gazetteer> villages  = new MultiSelectComboBox<>("Village | ភូមិ");

    private boolean syncing = false;

    public GazetteerMultiSearchField(GazetteerService service) {
        this(service, LayoutMode.RESPONSIVE);
    }

    public GazetteerMultiSearchField(GazetteerService service, LayoutMode mode) {
        this.service = Objects.requireNonNull(service);

        setWidthFull();

        configure(provinces);
        configure(districts);
        configure(communes);
        configure(villages);

        provinces.setItems(service.listByLevel(1));

        districts.setEnabled(false);
        communes.setEnabled(false);
        villages.setEnabled(false);

        wire();
        add(buildLayout(mode));
    }

    private Component buildLayout(LayoutMode mode) {
        provinces.setWidthFull();
        districts.setWidthFull();
        communes.setWidthFull();
        villages.setWidthFull();

        if (mode == LayoutMode.VERTICAL) {
            VerticalLayout vl = new VerticalLayout(provinces, districts, communes, villages);
            vl.setPadding(false); vl.setSpacing(true); vl.setMargin(false); vl.setWidthFull();
            return vl;
        }
        if (mode == LayoutMode.HORIZONTAL) {
            HorizontalLayout hl = new HorizontalLayout(provinces, districts, communes, villages);
            hl.setPadding(false); hl.setSpacing(true); hl.setMargin(false); hl.setWidthFull();
            hl.setFlexGrow(1, provinces, districts, communes, villages);
            return hl;
        }

        FormLayout fl = new FormLayout(provinces, districts, communes, villages);
        fl.setWidthFull();
        fl.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3),
                new FormLayout.ResponsiveStep("1300px", 4)
        );
        return fl;
    }

    private void configure(MultiSelectComboBox<Gazetteer> ms) {
        ms.setClearButtonVisible(true);
        ms.setPlaceholder("Please select...");
        ms.setItemLabelGenerator(g ->
                g == null ? "" : safe(g.getNameEn()) + " | " + safe(g.getNameKh()) + " (" + safe(g.getCode()) + ")"
        );
    }

    private void wire() {

        provinces.addValueChangeListener(e -> {
            if (syncing) return;
            syncing = true;
            try {
                // reset lower levels
                districts.clear(); communes.clear(); villages.clear();
                districts.setItems(List.of()); communes.setItems(List.of()); villages.setItems(List.of());

                if (provinces.getValue() == null || provinces.getValue().isEmpty()) {
                    districts.setEnabled(false);
                    communes.setEnabled(false);
                    villages.setEnabled(false);
                } else {
                    districts.setEnabled(true);
                    districts.setItems(loadChildrenOfSet(provinces.getValue()));
                    communes.setEnabled(false);
                    villages.setEnabled(false);
                }
            } finally {
                syncing = false;
            }
        });

        districts.addValueChangeListener(e -> {
            if (syncing) return;
            syncing = true;
            try {
                communes.clear(); villages.clear();
                communes.setItems(List.of()); villages.setItems(List.of());

                if (districts.getValue() == null || districts.getValue().isEmpty()) {
                    communes.setEnabled(false);
                    villages.setEnabled(false);
                } else {
                    communes.setEnabled(true);
                    communes.setItems(loadChildrenOfSet(districts.getValue()));
                    villages.setEnabled(false);
                }
            } finally {
                syncing = false;
            }
        });

        communes.addValueChangeListener(e -> {
            if (syncing) return;
            syncing = true;
            try {
                villages.clear();
                villages.setItems(List.of());

                if (communes.getValue() == null || communes.getValue().isEmpty()) {
                    villages.setEnabled(false);
                } else {
                    villages.setEnabled(true);
                    villages.setItems(loadChildrenOfSet(communes.getValue()));
                }
            } finally {
                syncing = false;
            }
        });
    }

    private List<Gazetteer> loadChildrenOfSet(Set<Gazetteer> parents) {
        if (parents == null || parents.isEmpty()) return List.of();
        // union children (distinct by id)
        Map<Long, Gazetteer> map = new LinkedHashMap<>();
        for (Gazetteer p : parents) {
            if (p == null || p.getId() == null) continue;
            for (Gazetteer ch : service.listChildren(p.getId())) {
                if (ch != null && ch.getId() != null) map.putIfAbsent(ch.getId(), ch);
            }
        }
        return new ArrayList<>(map.values());
    }

    public void clearAll() {
        syncing = true;
        try {
            provinces.clear();
            districts.clear();
            communes.clear();
            villages.clear();

            districts.setItems(List.of());
            communes.setItems(List.of());
            villages.setItems(List.of());

            districts.setEnabled(false);
            communes.setEnabled(false);
            villages.setEnabled(false);
        } finally {
            syncing = false;
        }
    }

    public Set<Gazetteer> getSelectedProvinces() { return provinces.getValue(); }
    public Set<Gazetteer> getSelectedDistricts() { return districts.getValue(); }
    public Set<Gazetteer> getSelectedCommunes()  { return communes.getValue(); }
    public Set<Gazetteer> getSelectedVillages()  { return villages.getValue(); }

    @Override protected Void generateModelValue() { return null; }
    @Override protected void setPresentationValue(Void value) { /* no-op */ }

    private static String safe(String s) { return s == null ? "" : s; }
}

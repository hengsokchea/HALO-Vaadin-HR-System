package org.halocambodia.component;

import java.util.List;
import java.util.Objects;

import org.halocambodia.data.Gazetteer;
import org.halocambodia.services.GazetteerService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class GazetteerField extends CustomField<Gazetteer> {

    public enum LayoutMode { RESPONSIVE, VERTICAL, HORIZONTAL }

    private final GazetteerService service;

    private final ComboBox<Gazetteer> province = new ComboBox<>("Province | ខេត្ត");
    private final ComboBox<Gazetteer> district = new ComboBox<>("District​​ | ស្រុក");
    private final ComboBox<Gazetteer> commune  = new ComboBox<>("Commune​​ | ឃុំ");
    private final ComboBox<Gazetteer> village  = new ComboBox<>("Village​​ | ភូមិ");

    private boolean syncing = false;

    public GazetteerField(GazetteerService service) {
        this(service, LayoutMode.RESPONSIVE);
    }

    public GazetteerField(GazetteerService service, LayoutMode mode) {
        this.service = Objects.requireNonNull(service);

        setWidthFull();

        configureCombo(province);
        configureCombo(district);
        configureCombo(commune);
        configureCombo(village);

        // Only 4 levels
        province.setItems(service.listByLevel(1)); // Provinces
        district.setEnabled(false);
        commune.setEnabled(false);
        village.setEnabled(false);

        wireListeners();

        add(buildLayout(mode));
    }

    private Component buildLayout(LayoutMode mode) {

        province.setWidthFull();
        district.setWidthFull();
        commune.setWidthFull();
        village.setWidthFull();

        if (mode == LayoutMode.VERTICAL) {
            VerticalLayout vl = new VerticalLayout(province, district, commune, village);
            vl.setPadding(false);
            vl.setSpacing(true);
            vl.setMargin(false);
            vl.setWidthFull();
            return vl;
        }

        if (mode == LayoutMode.HORIZONTAL) {
            HorizontalLayout hl = new HorizontalLayout(province, district, commune, village);
            hl.setPadding(false);
            hl.setSpacing(true);
            hl.setMargin(false);
            hl.setWidthFull();
            hl.setFlexGrow(1, province, district, commune, village);
            return hl;
        }

        // RESPONSIVE (default)
        FormLayout fl = new FormLayout(province, district, commune, village);
        fl.setWidthFull();
        fl.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3),
                new FormLayout.ResponsiveStep("1300px", 4) // optional: all 4 in one row on wide screens
        );
        return fl;
    }

    private void configureCombo(ComboBox<Gazetteer> cb) {
        cb.setClearButtonVisible(true);
        cb.setPlaceholder("Please select...");
        cb.setItemLabelGenerator(g ->
                g == null ? "" : safe(g.getNameEn()) + " | " + safe(g.getNameKh()) + " (" + safe(g.getCode()) + ")"
        );
        cb.setWidthFull();
    }

    private void wireListeners() {

        province.addValueChangeListener(e -> {
            if (syncing) return;

            Gazetteer p = e.getValue();

            syncing = true;
            try {
                district.clear();
                commune.clear();
                village.clear();

                district.setItems(List.of());
                commune.setItems(List.of());
                village.setItems(List.of());

                if (p == null) {
                    district.setEnabled(false);
                    commune.setEnabled(false);
                    village.setEnabled(false);
                } else {
                    district.setEnabled(true);
                    district.setItems(service.listChildren(p.getId())); // districts of province
                    commune.setEnabled(false);
                    village.setEnabled(false);
                }
            } finally {
                syncing = false;
            }

            updateModelValue();
        });

        district.addValueChangeListener(e -> {
            if (syncing) return;

            Gazetteer d = e.getValue();

            syncing = true;
            try {
                commune.clear();
                village.clear();

                commune.setItems(List.of());
                village.setItems(List.of());

                if (d == null) {
                    commune.setEnabled(false);
                    village.setEnabled(false);
                } else {
                    commune.setEnabled(true);
                    commune.setItems(service.listChildren(d.getId())); // communes of district
                    village.setEnabled(false);
                }
            } finally {
                syncing = false;
            }

            updateModelValue();
        });

        commune.addValueChangeListener(e -> {
            if (syncing) return;

            Gazetteer c = e.getValue();

            syncing = true;
            try {
                village.clear();
                village.setItems(List.of());

                if (c == null) {
                    village.setEnabled(false);
                } else {
                    village.setEnabled(true);
                    village.setItems(service.listChildren(c.getId())); // villages of commune
                }
            } finally {
                syncing = false;
            }

            updateModelValue();
        });

        village.addValueChangeListener(e -> {
            if (syncing) return;
            updateModelValue();
        });
    }

    private void updateModelValue() {
        Gazetteer v = village.getValue();
        Gazetteer c = commune.getValue();
        Gazetteer d = district.getValue();
        Gazetteer p = province.getValue();

        setModelValue(v != null ? v : (c != null ? c : (d != null ? d : p)), true);
    }

    @Override
    protected Gazetteer generateModelValue() {
        Gazetteer v = village.getValue();
        Gazetteer c = commune.getValue();
        Gazetteer d = district.getValue();
        Gazetteer p = province.getValue();
        return v != null ? v : (c != null ? c : (d != null ? d : p));
    }

    @Override
    protected void setPresentationValue(Gazetteer value) {
        syncing = true;
        try {
            // Clear all first
            province.clear();
            district.clear();
            commune.clear();
            village.clear();

            district.setItems(List.of());
            commune.setItems(List.of());
            village.setItems(List.of());

            district.setEnabled(false);
            commune.setEnabled(false);
            village.setEnabled(false);

            if (value == null || value.getId() == null) {
                return;
            }

            // Build chain Province -> District -> Commune -> Village
            Gazetteer node = value;

            Gazetteer v = null, c = null, d = null, p = null;

            while (node != null) {
                Integer lv = node.getLevel();
                if (lv != null) {
                    if (lv == 4) v = node;
                    else if (lv == 3) c = node;
                    else if (lv == 2) d = node;
                    else if (lv == 1) p = node;
                }
                node = node.getParent();
            }

            // Province (items already loaded)
            if (p != null) {
                province.setValue(p);
            }

            // District
            if (p != null) {
                district.setEnabled(true);
                List<Gazetteer> districts = service.listChildren(p.getId());
                district.setItems(districts);
                if (d != null) district.setValue(findInList(districts, d));
            }

            // Commune
            if (d != null) {
                commune.setEnabled(true);
                List<Gazetteer> communes = service.listChildren(d.getId());
                commune.setItems(communes);
                if (c != null) commune.setValue(findInList(communes, c));
            }

            // Village
            if (c != null) {
                village.setEnabled(true);
                List<Gazetteer> villages = service.listChildren(c.getId());
                village.setItems(villages);
                if (v != null) village.setValue(findInList(villages, v));
            }

        } finally {
            syncing = false;
        }
    }

    // ---------- helpers ----------

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static Gazetteer findInList(List<Gazetteer> list, Gazetteer wanted) {
        if (wanted == null || wanted.getId() == null) return null;
        return list.stream()
                .filter(x -> x != null && x.getId() != null && x.getId().equals(wanted.getId()))
                .findFirst()
                .orElse(null);
    }
}

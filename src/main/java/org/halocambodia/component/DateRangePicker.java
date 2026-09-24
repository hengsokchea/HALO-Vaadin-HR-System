package org.halocambodia.component;

import java.time.LocalDate;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

public class DateRangePicker extends HorizontalLayout {
    private final DatePicker from = new DatePicker();
    private final DatePicker to = new DatePicker();

    public DateRangePicker() {
        setSpacing(true);
        setPadding(false);
        setWidthFull();

        // ✅ force one line (no wrapping)
        getStyle()
            .set("display", "flex")
            .set("flex-wrap", "nowrap")
            .set("align-items", "baseline");

        from.setPlaceholder("From | ចាប់ពី");
        to.setPlaceholder("To | ដល់");

        // ✅ let them flex (don’t force 100% each)
        from.setWidth(null);
        to.setWidth(null);
        from.getStyle().set("min-width", "0");
        to.getStyle().set("min-width", "0");

        Span dash = new Span(" - ");
        dash.getStyle()
            .set("white-space", "nowrap")
            .set("flex", "0 0 auto");

        add(from, dash, to);

        setFlexGrow(1, from, to);

        from.addValueChangeListener(e -> syncMinMax());
        to.addValueChangeListener(e -> ensureToNotBeforeFrom());

        syncMinMax();
    }

    private void syncMinMax() {
        LocalDate f = from.getValue();
        to.setMin(f);
        ensureToNotBeforeFrom();
    }

    private void ensureToNotBeforeFrom() {
        LocalDate f = from.getValue();
        LocalDate t = to.getValue();
        if (f != null && t != null && t.isBefore(f)) {
            to.setValue(f);
        }
    }

    public LocalDate getFrom() { return from.getValue(); }
    public LocalDate getTo() { return to.getValue(); }

    public DatePicker getFromPicker() { return from; }
    public DatePicker getToPicker() { return to; }

    public void clear() {
        from.clear();
        to.clear();
        to.setMin(null);
    }
}

package org.halocambodia.views.training;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import org.halocambodia.views.MainLayout;

@Route(value = "training/reports", layout = MainLayout.class)
@PageTitle("Training Reports")
@PermitAll
public class TrainingReportView extends VerticalLayout {
    public TrainingReportView() {
        add("Training Reports");
    }
}

package org.halocambodia.views.policy;

import org.halocambodia.data.*;
import org.halocambodia.services.*;
import org.halocambodia.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "policy-acknowledgements", layout = MainLayout.class)
@PageTitle("My Policy Acknowledgements")
@PermitAll
public class PolicyAcknowledgeView extends VerticalLayout {

    public PolicyAcknowledgeView(PolicyReadService service) {
        setSizeFull();
        add(new H2("My Policy Acknowledgements"));

        Grid<PolicyReadDto> grid = new Grid<>(PolicyReadDto.class, false);
        grid.addColumn(PolicyReadDto::code).setHeader("Code").setAutoWidth(true).setResizable(true);
        grid.addColumn(PolicyReadDto::titleEn).setHeader("Title(EN)").setFlexGrow(1).setResizable(true);
        grid.addColumn(PolicyReadDto::titleKh).setHeader("Title(KH)").setFlexGrow(1).setResizable(true);
        grid.addColumn(PolicyReadDto::categoryNameEn).setHeader("Category").setFlexGrow(1).setResizable(true);
        grid.addColumn(PolicyReadDto::version).setHeader("Version").setAutoWidth(true).setResizable(true);
        grid.addColumn(p -> p.requiredAcknowledge() ? "Required" : "Not required").setHeader("Requirement").setAutoWidth(true).setResizable(true);
        grid.addColumn(p -> p.acknowledgedCurrentVersion() ? "Acknowledged" : "Pending").setHeader("Status").setAutoWidth(true).setResizable(true);
        grid.addColumn(PolicyReadDto::acknowledgedAt).setHeader("Acknowledged At").setAutoWidth(true).setResizable(true);
        grid.addComponentColumn(policy -> {
            Button openButton = new Button("Open",VaadinIcon.EXTERNAL_LINK.create());
            openButton.addClickListener(event ->
                    event.getSource()
                            .getUI()
                            .ifPresent(ui ->
                                    ui.navigate(
                                            PolicyReadView.class,
                                            policy.publicToken().toString()
                                    )
                            )
            );

            return openButton;
        })
        .setHeader("Action")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setResizable(true);
        grid.setItems(service.findAllAvailable());
        add(grid);
    }
}

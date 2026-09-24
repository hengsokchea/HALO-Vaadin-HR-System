package org.halocambodia.views.leave_management;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveRepository;
import org.halocambodia.data.LeaveActionToken;
import org.halocambodia.data.LeaveActionTokenRepository;
import org.halocambodia.data.LeaveStatusRepository;
import org.halocambodia.gmail.EmailService;
import org.halocambodia.services.LeaveAuditService;
import org.halocambodia.services.LeaveTokenService;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;

import jakarta.annotation.security.PermitAll;

@PageTitle("Leave Action")
@Route("leave-action/:token")
@PermitAll
public class LeaveActionView extends VerticalLayout implements BeforeEnterObserver {

    private final LeaveActionTokenRepository tokenRepo;
    private final EmployeeLeaveRepository leaveRepo;
    private final LeaveStatusRepository leaveStatusRepository;
    private final LeaveTokenService leaveTokenService;
    private final LeaveAuditService leaveAuditService;
    private final EmailService emailService;

    public LeaveActionView(
            LeaveActionTokenRepository tokenRepo,
            EmployeeLeaveRepository leaveRepo,
            LeaveStatusRepository leaveStatusRepository,
            LeaveTokenService leaveTokenService,
            LeaveAuditService leaveAuditService,
            EmailService emailService
    ) {

        this.tokenRepo = tokenRepo;
        this.leaveRepo = leaveRepo;
        this.leaveStatusRepository = leaveStatusRepository;
        this.leaveTokenService = leaveTokenService;
        this.leaveAuditService =leaveAuditService;
        this.emailService=emailService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle()
                .set("background", "#f4f6f9")
                .set("padding", "20px");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        String token =event.getRouteParameters().get("token") .orElse(null);

        removeAll();

        if (token == null || token.isEmpty()) {
            showResult("Invalid Request","Token is missing.", VaadinIcon.CLOSE_CIRCLE,"#e74c3c");
            return;
        }

        String tokenHash = leaveTokenService.sha256(token);

        LeaveActionToken t = tokenRepo.findByTokenHash(tokenHash) .orElse(null);

        // INVALID
        if (t == null) {

            showResult(
                    "Invalid Link",
                    "This approval link is invalid.",
                    VaadinIcon.CLOSE_CIRCLE,
                    "#e74c3c"
            );
            
            leaveAuditService.log(
                    VaadinServletRequest.getCurrent(),
                    t,
                    null,
                    false
            );

            return;
        }

        // USED
        if (t.isUsed()) {

            showResult(
                    "Already Used",
                    "This approval link was already used.",
                    VaadinIcon.WARNING,
                    "#f39c12"
            );

            return;
        }

        // EXPIRED
        if (t.getExpiresAt().isBefore(LocalDateTime.now())) {

            showResult(
                    "Expired",
                    "This approval link has expired.",
                    VaadinIcon.TIME_BACKWARD,
                    "#e67e22"
            );

            return;
        }

        try {

            EmployeeLeave leave =t.getEmployeeLeave();

            // APPROVE
            if ("APPROVE".equals(t.getActionType())) {

                leave.setLineManagerChecked(
                        leave.getLineManager());

                leave.setLineManagerCheckedPositions(
                        leave.getLineManagerPositions());

                leave.setLineManagerCheckDate(
                        LocalDate.now());

                leave.setLineManagerCheckedStatus(
                        leaveStatusRepository
                                .findById(2L)
                                .orElseThrow());

            } else {

                // REJECT
                leave.setLineManagerChecked(leave.getLineManager());

                leave.setLineManagerCheckedPositions(leave.getLineManagerPositions());

                leave.setLineManagerCheckDate(LocalDate.now());             
                leave.setLineManagerCheckedStatus(
                        leaveStatusRepository
                                .findById(3L)
                                .orElseThrow());
            }

            leaveRepo.save(leave);

            // MARK TOKEN USED
            t.setUsed(true);

            t.setUsedAt(LocalDateTime.now());

            tokenRepo.save(t);

            // SUCCESS
            showResult(
                    "Leave " + t.getActionType(),
                    "The leave request has been processed successfully.",
                    VaadinIcon.CHECK_CIRCLE,
                    "#27ae60"
            );
            leaveAuditService.log(
                    VaadinServletRequest.getCurrent(),
                    t,
                    leave,
                    true
            );
            
            if(leave.getEmployee().getOfficialEmail().length()>9) {
            	emailService.sendLeaveActionResult( leave, t.getActionType() );
            }

        } catch (Exception ex) {

            ex.printStackTrace();

            showResult(
                    "System Error",
                    ex.getMessage(),
                    VaadinIcon.CLOSE_CIRCLE,
                    "#c0392b"
            );
        }
    }

    private void showResult(
            String title,
            String message,
            VaadinIcon iconName,
            String color
    ) {

        VerticalLayout card = new VerticalLayout();

        card.setWidth("420px");

        card.setPadding(true);

        card.setSpacing(true);

        card.setAlignItems(Alignment.CENTER);

        card.getStyle()
                .set("background", "white")
                .set("border-radius", "16px")
                .set("box-shadow", "0 4px 20px rgba(0,0,0,0.08)")
                .set("padding", "40px");

        Icon icon = iconName.create();

        icon.setSize("64px");

        icon.setColor(color);

        H1 h1 = new H1(title);

        h1.getStyle()
                .set("margin", "0")
                .set("font-size", "28px");

        Paragraph p =
                new Paragraph(message);

        p.getStyle()
                .set("color", "#666")
                .set("text-align", "center");

        Button close =
                new Button("Close");

        close.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY);

        close.addClickListener(e -> {
            getUI().ifPresent(ui ->
                    ui.getPage()
                            .executeJs("window.close();"));
        });

        card.add(
                icon,
                h1,
                p,
                close
        );

        add(card);
    }
}
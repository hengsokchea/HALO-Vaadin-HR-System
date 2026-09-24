package org.halocambodia.views;

import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.shared.Tooltip.TooltipPosition;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.streams.DownloadHandler;

import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Horizontal;

import jakarta.annotation.security.PermitAll;

import org.halocambodia.component.AnnualLeaveDashboardDialog;
import org.halocambodia.component.ThemeToggleButton;
import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.DataFeedsLogRepository;
import org.halocambodia.data.EmployeeAllocation;
import org.halocambodia.data.EmployeeLeaveBalance;
import org.halocambodia.data.EmployeeLeaveBalanceRepository;
import org.halocambodia.data.FileUploadUtility;
import org.halocambodia.data.User;
import org.halocambodia.data.UserRepository;
import org.halocambodia.data.UserWebAuthn;
import org.halocambodia.data.UserWebAuthnRepository;
import org.halocambodia.power_bi.PowerBIView;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.ChangePasswordDialog;
import org.halocambodia.security.WebAuthnService;
import org.halocambodia.views.admin.role_management.RoleManagementView;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.halocambodia.views.admin.user_management.UserProfileDialog;
import org.halocambodia.views.attachment_type.AttachmentTypeView;
import org.halocambodia.views.attendance_management.AttendanceFinalReviewView;
import org.halocambodia.views.attendance_management.AttendanceRegisterView;
import org.halocambodia.views.attendance_management.AttendanceSummaryView;
import org.halocambodia.views.attendance_management.AttendanceVerificationView;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.halocambodia.views.attendance_management.TimesheetEntryView;
import org.halocambodia.views.career.CareerTypeView;
import org.halocambodia.views.dashboard.DashboardView;
import org.halocambodia.views.data_feed.DataFeedView;
import org.halocambodia.views.department.DepartmentView;
import org.halocambodia.views.disability.DisabilityTypeOptionView;
import org.halocambodia.views.disability.DisabilityTypeView;
import org.halocambodia.views.disciplinary.DisciplinaryView;
import org.halocambodia.views.donors.ContractsView;
import org.halocambodia.views.donors.DonorView;
import org.halocambodia.views.driving_license.DrivingLicenseView;
import org.halocambodia.views.education.EducationCenterView;
import org.halocambodia.views.education.EducationQualificationsView;
import org.halocambodia.views.employee.InternationalStaffView;

import org.halocambodia.views.employee.NationalStaffActiveView;

import org.halocambodia.views.employee_allocate.EmployeeAllocationView;
import org.halocambodia.views.gazetteer.GazetteerView;
import org.halocambodia.views.goal_setting.GoalSettingEntryView;
import org.halocambodia.views.goal_setting.FinalGoalView;
import org.halocambodia.views.goal_setting.GoalSupervisorView;
import org.halocambodia.views.language.LanguageView;
import org.halocambodia.views.leave_management.FinalLeaveReviewView;
import org.halocambodia.views.leave_management.LeaveRequestView;
import org.halocambodia.views.leave_management.LeaveSupportingDocumentView;
import org.halocambodia.views.leave_management.LeaveTypeView;
import org.halocambodia.views.leave_management.SupervisorLeaveReviewView;
import org.halocambodia.views.payroll.PayrollComponentView;
import org.halocambodia.views.payroll.PayrollEmployeeAdjustmentsView;
import org.halocambodia.views.payroll.PayrollEmployeeResultView;
import org.halocambodia.views.payroll.PayrollNssfRuleView;
import org.halocambodia.views.payroll.PayrollPaymentSettingView;
import org.halocambodia.views.payroll.PayrollRuleView;
import org.halocambodia.views.payroll.PayrollSeniorityRuleView;
import org.halocambodia.views.payroll.PayrollTaxRateView;
import org.halocambodia.views.payroll.PayrollView;
import org.halocambodia.views.performance.EmployeePerformanceFinalReviewView;
import org.halocambodia.views.performance.EmployeePerformanceReviewView;
import org.halocambodia.views.performance.EmployeePerformanceSummaryView;
import org.halocambodia.views.policy.PolicyAccessLogView;
import org.halocambodia.views.policy.PolicyAcknowledgeView;
import org.halocambodia.views.policy.PolicyCategoryView;
import org.halocambodia.views.policy.PolicyView;
import org.halocambodia.views.position.PositionView;
import org.halocambodia.views.relationship.RelationshipView;
import org.halocambodia.views.religions.ReligionsView;
import org.halocambodia.views.reports.data_explorer_reports.DataExplorerView;
import org.halocambodia.views.reports.simple_reports.ReportsView;
import org.halocambodia.views.roster.CalendarView;
import org.halocambodia.views.roster.HolidayView;
import org.halocambodia.views.roster.RosterView;
import org.halocambodia.views.roster.ShiftView;
import org.halocambodia.views.salary_grade.PoGradeView;
import org.halocambodia.views.teams.TeamsView;
import org.halocambodia.views.training.EmployeeTrainingView;
import org.halocambodia.views.training.TrainingCourseView;
import org.halocambodia.views.training.TrainingReportView;
import org.halocambodia.views.vaccination.VaccinationTypeView;
import org.halocambodia.views.vaccination.VaccinationView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@PermitAll

public class MainLayout extends AppLayout implements AfterNavigationObserver {
    private static final Logger logger = LoggerFactory.getLogger(MainLayout.class);

    private final FileUploadUtility fileUploadUtility;
    private static final String UserProfileSubDirectory="userProfile";
    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;


    
    private H3 viewTitle;
    private AuthenticatedUser authenticatedUser;
    private AccessAnnotationChecker accessChecker;
    private final UserRepository userRepository;
    private final DataFeedsLogRepository dataFeedsLogRepository;
    
    private final WebAuthnService webAuthnService;
    private final UserWebAuthnRepository userWebAuthnRepository;
    
    private final WebAuthnBridge bridge;
    

    public MainLayout(AuthenticatedUser authenticatedUser, AccessAnnotationChecker accessChecker, UserRepository userRepository, FileUploadUtility fileUploadUtility,DataFeedsLogRepository dataFeedsLogRepository,EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository, WebAuthnService webAuthnService, UserWebAuthnRepository userWebAuthnRepository,WebAuthnBridge bridge) {
        this.authenticatedUser = authenticatedUser;
        this.accessChecker = accessChecker;
        this.userRepository = userRepository;
        this.dataFeedsLogRepository=dataFeedsLogRepository;
        this.fileUploadUtility = fileUploadUtility;
        this.employeeLeaveBalanceRepository=employeeLeaveBalanceRepository;
        this.webAuthnService = webAuthnService;
        this.userWebAuthnRepository = userWebAuthnRepository;
        this.bridge=bridge;

        
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();

        Optional<User> maybeUser = authenticatedUser.get();
        if (maybeUser.isEmpty()) {
            UI.getCurrent().navigate("login");
            return;
        }
        

    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");
        
        toggle.getStyle()
		//.set("color", "white")
		.set("margin-right", "8px");
		//.set("letter-spacing", "0.5px")
		//.set("font-weight", "600");
        

    
        viewTitle = new H3();
        viewTitle.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        viewTitle.setWidthFull();
        viewTitle.getStyle()
        .set("font-weight", "500")
        .set("letter-spacing", "0.3px")
        .set("text-overflow", "ellipsis")
        .set("overflow", "hidden")
        .set("white-space", "nowrap")
        .set("flex-grow", "1")
        .set("flex-shrink", "1")
        .set("min-width", "0");
        

        
       
        
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.setMargin(false);
        layout.setPadding(false);
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        layout.getStyle()
                .set("margin-left", "auto")
                .set("gap", "6px")
                .set("flex-shrink", "0");


        Long errorCount=dataFeedsLogRepository.countLogsWithErrorsAndParentAndActiveFeed();
        if (errorCount > 0) {
        	Span badge = new Span(errorCount.toString());
        	badge.getElement().getThemeList().add("badge error pill primary");

        	RouterLink link = new RouterLink();
        	link.add(badge);
        	link.setRoute(DataFeedView.class);
        	layout.add(link);
  
        }
  
        
        ThemeToggleButton themeToggleButton = new ThemeToggleButton();
        layout.add(themeToggleButton);
        
        Optional<User> maybeUser = authenticatedUser.get();


        
        if (maybeUser.isPresent()) {
            User user = maybeUser.get();

            String profileImageUrl =
                    fileUploadUtility.getAvatarImageUrl(
                            UserProfileSubDirectory,
                            user.getProfileImagePath()
                    );

            Avatar avatar = new Avatar(user.getName());
            avatar.setImage(profileImageUrl);
            avatar.setThemeName("small");
            avatar.getElement().setAttribute("tabindex", "-1");
            avatar.addClassName("profile-avatar");

            avatar.getStyle()
                    .set("border", "2px solid var(--halo-green)")
                    .set("color", "white")
                    .set(
                            "box-shadow",
                            "0 2px 6px rgba(0,0,0,.15)"
                    );

            Tooltip.forComponent(avatar)
                    .setText(user.getName());

            Component userMenu =
                    createUserMenu(user, avatar);

            // The avatar is added after the theme button
            layout.add(userMenu);

        } else {
            Anchor loginLink =
                    new Anchor("login", "Sign in");

            layout.add(loginLink);
        }

        layout.getStyle()
                .set("margin-left", "auto")
                .set("gap", "6px");



        addToNavbar(true,toggle,viewTitle,layout);
    }

    private Component createUserMenu(User user, Avatar avatar) {

        HorizontalLayout profileButton = new HorizontalLayout();
        profileButton.setSpacing(true);
        profileButton.setPadding(false);
        profileButton.setDefaultVerticalComponentAlignment(Alignment.CENTER);

       // Span userName = new Span(user.getName());

      //  userName.getStyle()
       //         .set("color", "white")
       //         .set("font-weight", "600");

     //   Icon dropdown = new Icon("lumo", "dropdown");

        //profileButton.add(avatar, userName, dropdown);
        profileButton.add(avatar);
        profileButton.getStyle().set("cursor", "pointer");
        profileButton.getElement().setAttribute("tabindex", "0");
        profileButton.getElement().setAttribute("role", "button");
        profileButton.getElement().setAttribute("aria-label", "Open user menu");
        profileButton.getElement().setAttribute("aria-haspopup", "menu");
        

        ContextMenu menu = new ContextMenu(profileButton);
        menu.setOpenOnClick(true);
        menu.setClassName("user-menu");
       
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.addClassName("user-menu-header");
        headerLayout.setAlignItems(Alignment.CENTER);

        Avatar headerAvatar = new Avatar(user.getName());
        headerAvatar.getStyle()
        .set("border", "3px solid var(--halo-green)")
        .set("box-shadow", "0 2px 6px rgba(0,0,0,.15)");
        headerAvatar.setImage(
            fileUploadUtility.getAvatarImageUrl(
                UserProfileSubDirectory,
                user.getProfileImagePath()
            )
        );

        VerticalLayout userInfo = new VerticalLayout();
        userInfo.setPadding(false);
        userInfo.setSpacing(false);

        userInfo.add(
            new Span(user.getName()),
            new Span(user.getEmail() == null ? "" : user.getEmail())
        );

        headerLayout.add(headerAvatar, userInfo);
        
        MenuItem header = menu.addItem("");
        header.addComponentAsFirst(headerLayout);
        header.setEnabled(false);

        menu.addSeparator();
        
        MenuItem profile = menu.addItem("Profile | ប្រវត្តិរូប", e -> openProfileDialog(Optional.of(user)) );
        profile.addComponentAsFirst(VaadinIcon.USER_CARD.create());

        MenuItem leaveBalance = menu.addItem("Leave Balance | សមតុល្យច្បាប់", e -> {
                    AnnualLeaveDashboardDialog dialog = new AnnualLeaveDashboardDialog(employeeLeaveBalanceRepository);
                    dialog.show(user);
                }
        );
        leaveBalance.addComponentAsFirst(VaadinIcon.CALENDAR_USER.create());

        MenuItem payrollResult = menu.addItem("Payslips | បង្កាន់ដៃប្រាក់ខែ", e -> UI.getCurrent().navigate(PayrollEmployeeResultView.class));
        payrollResult.addComponentAsFirst(VaadinIcon.MONEY.create());

        menu.addSeparator();

        MenuItem changePassword = menu.addItem("Password | ពាក្យសម្ងាត់", e -> openChangePasswordDialog(Optional.of(user)));
        changePassword.addComponentAsFirst(VaadinIcon.PASSWORD.create());

        MenuItem fingerprint = menu.addItem("Biometrics | ជីវមាត្រ", e -> openRegisterFingerprintDialog(user) );
        fingerprint.addComponentAsFirst( LineAwesomeIcon.FINGERPRINT_SOLID.create());

        menu.addSeparator();

        MenuItem signOut = menu.addItem("Sign Out | ចាកចេញ",e -> authenticatedUser.logout());
        signOut.addComponentAsFirst(VaadinIcon.SIGN_OUT.create());

        return profileButton;
    }
    
    

    private void openRegisterFingerprintDialog(User user) {

        CustomDialog dialog = new CustomDialog("ចុះឈ្មោះការចូលប្រើដោយជីវមាត្រ | Register Biometric Login");
        bridge.setRegistrationDialog(dialog);
        //dialog.setHeaderTitle("ចុះឈ្មោះការចូលប្រើដោយជីវមាត្រ | Register Biometric Login");
        //dialog.setResizable(true);
       // dialog.setDraggable(true);

        VerticalLayout layout = new VerticalLayout();

        SvgIcon fingerprintIcon = LineAwesomeIcon.FINGERPRINT_SOLID.create();
        fingerprintIcon.setSize("70px");
        fingerprintIcon.setColor("#008DA8");

        Span messageEn = new Span("Register biometric login using your fingerprint, face recognition, Windows Hello, Touch ID, or another supported authenticator.");
        Span messageKh = new Span("ចុះឈ្មោះការចូលប្រើដោយជីវមាត្រ ដោយប្រើស្នាមម្រាមដៃ ការស្គាល់មុខ Windows Hello, Touch ID ឬវិធីផ្ទៀងផ្ទាត់សុវត្ថិភាពផ្សេងៗដែលគាំទ្រ។");
        
        TextField deviceName = new TextField("ឈ្មោះឧបករណ៍ | Device Name");
        deviceName.setWidthFull();
        deviceName.setPlaceholder("e.g. Samsung S24, Work Laptop");


        SvgIcon fingerprintIconBn = LineAwesomeIcon.FINGERPRINT_SOLID.create();
        //fingerprintIconBn.setColor("#008DA8");
        
        Button registerBtn = new Button("ចុះឈ្មោះ | Register",fingerprintIconBn);

        registerBtn.addThemeVariants(ButtonVariant.LARGE,ButtonVariant.LUMO_PRIMARY);
        registerBtn.addClickListener(event -> {

            String challenge = webAuthnService.generateChallenge();

            VaadinSession.getCurrent()
                    .setAttribute(
                            "webauthn-register-challenge",
                            challenge
                    );

            dialog.getElement().executeJs("""
            
                function base64urlToUint8Array(base64url) {

                    const base64 =
                        base64url
                            .replace(/-/g, '+')
                            .replace(/_/g, '/');

                    const padded =
                        base64 +
                        '='.repeat((4 - base64.length % 4) % 4);

                    const binary = atob(padded);

                    return Uint8Array.from(
                        binary,
                        c => c.charCodeAt(0)
                    );
                }

                if (!window.PublicKeyCredential) {

                    alert("WebAuthn is not supported by this browser");
                    return;
                }

                const challengeBytes =
                    base64urlToUint8Array($0);

                const userIdBytes =
                    Uint8Array.from(
                        String($1),
                        c => c.charCodeAt(0)
                    );

                navigator.credentials.create({

                    publicKey: {
                        challenge: challengeBytes,
                        rp: {
                            name: "Cam-HRIS"  ,
                             id: window.location.hostname                          
                        },

                        user: {
                            id: userIdBytes,
                            name: $2,
                            displayName: $3
                        },

                        pubKeyCredParams: [
                            {
                                type: "public-key",
                                alg: -7
                            },
                            {
                                type: "public-key",
                                alg: -257
                            }
                        ],

                        authenticatorSelection: {

                            residentKey: "required",
            				userVerification: "required"
                        },

                        timeout: 600000,

                        attestation: "none"
                    },
                    
                        hints: [
				        "client-device",
				        "hybrid",
				        "security-key"
				    ]

                })

                .then(credential => {

                    const response = {

                        credentialId: credential.id,

                        rawId: btoa(
                            String.fromCharCode(
                                ...new Uint8Array(
                                    credential.rawId
                                )
                            )
                        ),

                        clientDataJSON: btoa(
                            String.fromCharCode(
                                ...new Uint8Array(
                                    credential.response.clientDataJSON
                                )
                            )
                        ),

                        attestationObject: btoa(
                            String.fromCharCode(
                                ...new Uint8Array(
                                    credential.response.attestationObject
                                )
                            )
                        ),
                        
                        transports: credential.response.getTransports ? credential.response.getTransports() : [],

                        userAgent: navigator.userAgent,
                        
                        userHandle: btoa(
					        String.fromCharCode(
					            ...userIdBytes
					        )
					    ),
					    deviceName: $5
        
                    };

                    console.log(
                        "WebAuthn Registration Response",
                        response
                    );

                    if (!$4 || !$4.$server) {

                        alert(
                            "Vaadin bridge not found"
                        );

                        return;
                    }

                    return $4.$server.saveCredential(
                        JSON.stringify(response)
                    );
                })

                .then(result => {

                    if (!result) {
                        return;
                    }

                    if (result === "SUCCESS") {
            		    return $4.$server.registrationSuccess();
                    } else {

                        alert(
                            "Registration failed: " +
                            result
                        );
                    }

                })

                .catch(error => {

                    console.error(
                        "WebAuthn Registration Error",
                        error
                    );

                    alert(
                        error.message ||
                        error.toString()
                    );
                });

                """,
                challenge,
                user.getId(),
                (user.getEmail()==null?"":user.getEmail()),
                user.getName(),
                bridge.getElement(),
                deviceName.getValue()
            );
        });

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        layout.setAlignItems(Alignment.CENTER);
        layout.add(fingerprintIcon,messageKh, messageEn,deviceName);

        dialog.add(layout);
      
        dialog.add(bridge);
        dialog.getFooter().add(registerBtn,cancelBtn);

        dialog.open();
    }
    



    public void openChangePasswordDialog(Optional<User> maybeUser) {
        maybeUser.ifPresent(user -> 
            new ChangePasswordDialog(user, userRepository).open()
        );
    }
    
    public void openProfileDialog(Optional<User> maybeUser) {
        maybeUser.ifPresent(user -> 
            new UserProfileDialog(user, userRepository,fileUploadUtility).open()
        );
    }

    private void addDrawerContent() {

        /*
         * Blue logo for light mode.
         */
        Image lightLogo = new Image(DownloadHandler.forClassResource(getClass(),"/assets/images/HALO_Logo_Dark.png"),"HALO logo");

        configureDrawerLogo(lightLogo,"drawer-logo-light");

        /*
         * White logo for dark mode.
         */
        Image darkLogo = new Image(DownloadHandler.forClassResource(getClass(),"/assets/images/HALO_Logo_Light.png"),"HALO logo");

        configureDrawerLogo(darkLogo,"drawer-logo-dark");

        /*
         * Application name.
         * Do not set the colour inline. CSS controls it.
         */
        Span appName = new Span("Cam-HRIS");

        appName.addClassNames(LumoUtility.FontWeight.BOLD,"app-name");

        appName.getStyle()
                .set("letter-spacing","0.6px")
                .set("font-size","clamp(15px, 1.35vw + 0.35rem, 21px)")
               //.set("margin-left","var(--lumo-space-m)")
                .set("white-space","nowrap");

        /*
         * Header content.
         */
        HorizontalLayout headerLayout = new HorizontalLayout(lightLogo,darkLogo, appName);
        headerLayout.addClassName("drawer-header-layout");
        headerLayout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        headerLayout.setJustifyContentMode(JustifyContentMode.CENTER);

        headerLayout.setWidthFull();
        headerLayout.setHeightFull();

        headerLayout.setPadding(false);
        headerLayout.setMargin(false);
        headerLayout.setSpacing(false);

        headerLayout.getStyle().set("cursor", "pointer");

        headerLayout.addClickListener(event ->
                UI.getCurrent().navigate(DashboardView.class)
        );

        /*
         * Drawer header.
         */
        Header header =new Header(headerLayout);

        header.setWidthFull();

        header.addClassNames(
                "drawer-header",
                "clickable-header"
        );

        header.getElement().setAttribute("role", "button");
        header.getElement().setAttribute("aria-label", "Go to Dashboard");
        header.getElement().setAttribute("tabindex","0");
        
        header.getElement().addEventListener("keydown",event -> UI.getCurrent().navigate(DashboardView.class)).setFilter("event.key === 'Enter' || event.key === ' '");

        /*
         * Navigation.
         */
        SideNav nav = createNavigation();

        nav.getStyle()
                .set("padding","0.5rem 0.25rem")
                .set("font-size","var(--lumo-font-size-s)");

        /*
         * Navigation scroller.
         */
        Scroller scroller = new Scroller(nav);

        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        scroller.setSizeFull();
        scroller.addClassName("scroller-bg");

        scroller.getStyle()
                .set("overflow-y", "auto")
                .set("flex", "1");

        /*
         * Complete drawer layout.
         */
        VerticalLayout drawerLayout =
                new VerticalLayout(
                        header,
                        scroller
                );

        drawerLayout.setPadding(false);
        drawerLayout.setMargin(false);
        drawerLayout.setSpacing(false);
        drawerLayout.setSizeFull();

        drawerLayout.getStyle()
                .set("overflow", "hidden");

        addToDrawer(drawerLayout);

 
    }

    private void configureDrawerLogo(
            Image logo,
            String className) {

        logo.addClassNames(
                "drawer-logo",
                className
        );

        logo.setWidth("64px");
        logo.setMinWidth("64px");
        logo.setMaxWidth("64px");

        logo.setHeight("44px");
        logo.setMinHeight("44px");
        logo.setMaxHeight("44px");

        logo.getStyle()
                .set("object-fit", "contain")
                .set("object-position", "center")
                .set("flex-shrink", "0");
    }



    private SideNav createNavigation() {
       // Optional<User> maybeUser = authenticatedUser.get();
        SideNav nav = new SideNav();
   

        
        SideNavItem powerBISection = new SideNavItem("PowerBI Dashboard", PowerBIView.class, VaadinIcon.CHART_LINE.create());
        Tooltip tipPowerBISection = Tooltip.forComponent(powerBISection);
        tipPowerBISection.setText("Interactive PowerBI dashboards and analytics");
        tipPowerBISection.setPosition(TooltipPosition.END);
        tipPowerBISection.setHideDelay(200);
        nav.addItem(powerBISection);
        
    

        

        // === Employee Management ===
        SideNavItem employeeManagementSection = new SideNavItem("Employee Management");
        employeeManagementSection.setPrefixComponent(VaadinIcon.USERS.create());
        Tooltip tipEmp = Tooltip.forComponent(employeeManagementSection);
        tipEmp.setText("Manage employee records, profiles, and positions");
        tipEmp.setPosition(TooltipPosition.END);
        tipEmp.setHideDelay(200);
        nav.addItem(employeeManagementSection);

        
        SideNavItem menuItemNationalStaffActive = new SideNavItem("National Staff", NationalStaffActiveView.class, VaadinIcon.USER_CHECK.create());
        Tooltip tipMenuItemNationalStaffActive = Tooltip.forComponent(menuItemNationalStaffActive);
        tipMenuItemNationalStaffActive.setText("View and manage active national staff");
        tipMenuItemNationalStaffActive.setPosition(TooltipPosition.END);
        tipMenuItemNationalStaffActive.setHideDelay(200);
        employeeManagementSection.addItem(menuItemNationalStaffActive);

        
        SideNavItem menuItemInternationalStaffActive = new SideNavItem("International Staff", InternationalStaffView.class, VaadinIcon.GLOBE.create());
        Tooltip tipMenuItemInternationalStaffActive = Tooltip.forComponent(menuItemInternationalStaffActive);
        tipMenuItemInternationalStaffActive.setText("View and manage active international staff");
        tipMenuItemInternationalStaffActive.setPosition(TooltipPosition.END);
        tipMenuItemInternationalStaffActive.setHideDelay(200);
        employeeManagementSection.addItem(menuItemInternationalStaffActive);

  
        
     // === Staff Development & Training ===
        SideNavItem staffDevTrainingSection = new SideNavItem("Staff Development");
        staffDevTrainingSection.setPrefixComponent(VaadinIcon.ACADEMY_CAP.create());
        Tooltip tipStaffDevTraining = Tooltip.forComponent(staffDevTrainingSection);
        tipStaffDevTraining.setText("Training courses catalog and employee training records");
        tipStaffDevTraining.setPosition(TooltipPosition.END);
        tipStaffDevTraining.setHideDelay(200);
        nav.addItem(staffDevTrainingSection);
        //employeeManagementSection.addItem(staffDevTrainingSection);
        


        SideNavItem trainingCoursesSetting = new SideNavItem("Training Courses", TrainingCourseView.class, VaadinIcon.BOOK.create());
        Tooltip tipTrainingCoursesSetting = Tooltip.forComponent(trainingCoursesSetting);
        tipTrainingCoursesSetting.setText("Training course master data (catalog)");
        tipTrainingCoursesSetting.setPosition(TooltipPosition.END);
        tipTrainingCoursesSetting.setHideDelay(200);
        staffDevTrainingSection.addItem(trainingCoursesSetting);
        

        SideNavItem employeeTrainingRecords = new SideNavItem("Employee Training Records", EmployeeTrainingView.class, VaadinIcon.USERS.create());
        Tooltip tipEmployeeTrainingRecords = Tooltip.forComponent(employeeTrainingRecords);
        tipEmployeeTrainingRecords.setText("Track who attended which training, dates, status, certificates");
        tipEmployeeTrainingRecords.setPosition(TooltipPosition.END);
        tipEmployeeTrainingRecords.setHideDelay(200);
        staffDevTrainingSection.addItem(employeeTrainingRecords);



        // === Performance / Goal Setting ===
        SideNavItem performanceGoalSettingSection = new SideNavItem("Performance");
        performanceGoalSettingSection.setPrefixComponent(VaadinIcon.TROPHY.create());
        Tooltip tipPerf = Tooltip.forComponent(performanceGoalSettingSection);
        tipPerf.setText("Set, review, and finalize employee performance goals");
        tipPerf.setPosition(TooltipPosition.END);
        tipPerf.setHideDelay(200);
        nav.addItem(performanceGoalSettingSection);
        
        SideNavItem redundancySection = new SideNavItem("Redundancy");
        redundancySection.setPrefixComponent(VaadinIcon.WARNING.create());
        Tooltip tipRedanceSection = Tooltip.forComponent(redundancySection);
        tipRedanceSection.setText("Manage redundancy cases: assessment, review, and approval | គ្រប់គ្រងការកាត់បន្ថយបុគ្គលិក៖ វាយតម្លៃ ពិនិត្យ និងអនុម័ត");
        tipRedanceSection.setPosition(TooltipPosition.END);
        tipRedanceSection.setHideDelay(200);
        performanceGoalSettingSection.addItem(redundancySection);
        
        
        SideNavItem performanceReview = new SideNavItem("Performance Review", EmployeePerformanceReviewView.class, VaadinIcon.USER_CHECK.create());
        Tooltip tipPerformanceReview = Tooltip.forComponent(performanceReview);
        tipPerformanceReview.setText("Supervisor review & scoring | អ្នកគ្រប់គ្រងពិនិត្យ និងដាក់ពិន្ទុ");
        tipPerformanceReview.setPosition(TooltipPosition.END);
        tipPerformanceReview.setHideDelay(200);
        redundancySection.addItem(performanceReview);
        
        
        SideNavItem finalPerformanceReview = new SideNavItem("Final Review", EmployeePerformanceFinalReviewView.class, VaadinIcon.CLIPBOARD_CHECK.create());
        Tooltip tipFinalPerformanceReview = Tooltip.forComponent(finalPerformanceReview);
        tipFinalPerformanceReview.setText("Final review & approval | ពិនិត្យចុងក្រោយ និងអនុម័ត");
        tipFinalPerformanceReview.setPosition(TooltipPosition.END);
        tipFinalPerformanceReview.setHideDelay(200);
        redundancySection.addItem(finalPerformanceReview);
        
        SideNavItem performanceSummary = new SideNavItem("Performance Summary", EmployeePerformanceSummaryView.class, VaadinIcon.CHART.create());
        Tooltip tipPerformanceSummary = Tooltip.forComponent(performanceSummary);
        tipPerformanceSummary.setText("Summary and results overview | សង្ខេប និងទិដ្ឋភាពលទ្ធផល");
        tipPerformanceSummary.setPosition(TooltipPosition.END);
        tipPerformanceSummary.setHideDelay(200);
        redundancySection.addItem(performanceSummary);
        

	     // =========================================================
	     // GOAL SETTING
	     // =========================================================
	    SideNavItem goalSettingSection =new SideNavItem("Goal Setting");
	    goalSettingSection.setPrefixComponent( VaadinIcon.TASKS.create());
	    addTooltip(goalSettingSection,"Create, review and finalize annual employee goals");
	
	     if (authenticatedUser.hasPage(GoalSettingEntryView.class,AccessPageType.SELECTED_PAGE)) {
	         SideNavItem goalEntry =new SideNavItem("Goal Entry",GoalSettingEntryView.class,VaadinIcon.CLIPBOARD_TEXT.create());
	         addTooltip(goalEntry,"Employees create and submit their annual goals");
	         goalSettingSection.addItem(goalEntry);
	     }
	
	     // ---------------------------------------------------------
	     // Supervisor Goal Review
	     // ---------------------------------------------------------
	     if (authenticatedUser.hasPage(GoalSupervisorView.class, AccessPageType.SELECTED_PAGE)) {
	         SideNavItem supervisorGoalReview = new SideNavItem("Supervisor Review",GoalSupervisorView.class,VaadinIcon.USER_CHECK.create());
	         addTooltip(supervisorGoalReview,"Supervisors review and approve employee goals");
	         goalSettingSection.addItem(supervisorGoalReview);         
	     }
	
	     // ---------------------------------------------------------
	     // Final Goal Review
	     // ---------------------------------------------------------
	     if (authenticatedUser.hasPage(FinalGoalView.class,AccessPageType.SELECTED_PAGE)) {
	    	 SideNavItem finalGoalReview = new SideNavItem("Final Review",FinalGoalView.class, VaadinIcon.CHECK_SQUARE.create());
	         addTooltip(finalGoalReview,"HR performs the final review and approval");        
	         goalSettingSection.addItem(finalGoalReview);
	         
	     }


     	performanceGoalSettingSection.addItem(goalSettingSection);

     

        // === Shift & Roster ===
        SideNavItem shiftRosterSection = new SideNavItem("Holiday & Calendar");
        shiftRosterSection.setPrefixComponent(VaadinIcon.CLOCK.create());
        Tooltip tipShiftRoster = Tooltip.forComponent(shiftRosterSection);
        tipShiftRoster.setText("Plan and manage team shifts, rosters, and staff allocations");
        tipShiftRoster.setPosition(TooltipPosition.END);
        tipShiftRoster.setHideDelay(200);
        nav.addItem(shiftRosterSection);
        
        SideNavItem publicHoliday = new SideNavItem("Holiday Calendar", HolidayView.class, VaadinIcon.FLAG.create());
        Tooltip tipPublicHoliday = Tooltip.forComponent(publicHoliday);
        tipPublicHoliday.setText("Manage public holidays | គ្រប់គ្រងថ្ងៃឈប់សម្រាក");
        tipPublicHoliday.setPosition(TooltipPosition.END);
        tipPublicHoliday.setHideDelay(200);

        shiftRosterSection.addItem(publicHoliday);

        

        SideNavItem shift = new SideNavItem("Working Hours", ShiftView.class, VaadinIcon.CALENDAR_CLOCK.create());
        Tooltip tipShift = Tooltip.forComponent(shift);
        tipShift.setText("Define work shifts and their schedules");
        tipShift.setPosition(TooltipPosition.END);
        tipShift.setHideDelay(200);
        shiftRosterSection.addItem(shift);


        SideNavItem calendar = new SideNavItem("Calendar", CalendarView.class, VaadinIcon.CALENDAR.create());
        Tooltip tipCalendar = Tooltip.forComponent(calendar);
        tipCalendar.setText("Generate Shift Calendar | បង្កើតប្រតិទិនតាមវេន");
        tipCalendar.setPosition(TooltipPosition.END);
        tipCalendar.setHideDelay(200);
        shiftRosterSection.addItem(calendar);
        
        


        

        

     // === Deployment Management ===
        SideNavItem deploymentSection = new SideNavItem("Deployment Management");
        deploymentSection.setPrefixComponent(VaadinIcon.SUITCASE .create());

        Tooltip tipDeploymentSection = Tooltip.forComponent(deploymentSection);
        tipDeploymentSection.setText("Plan and manage team deployments and staff allocations");
        tipDeploymentSection.setPosition(TooltipPosition.END);
        tipDeploymentSection.setHideDelay(200);

        nav.addItem(deploymentSection);

        SideNavItem empAlloc = new SideNavItem("Team Deployment Plan", EmployeeAllocationView.class, VaadinIcon.USERS.create());
        Tooltip tipAlloc = Tooltip.forComponent(empAlloc);
        tipAlloc.setText("Assign employees to location, teams, positions, and shift cycles");
        tipAlloc.setPosition(TooltipPosition.END);
        tipAlloc.setHideDelay(200);

        deploymentSection.addItem(empAlloc);
        
        SideNavItem roster = new SideNavItem("Roster", RosterView.class, VaadinIcon.USER_CLOCK.create());
        Tooltip tipRoster = Tooltip.forComponent(roster);
        tipRoster.setText("Generate and manage employee rosters");
        tipRoster.setPosition(TooltipPosition.END);
        tipRoster.setHideDelay(200);
        deploymentSection.addItem(roster);
        
        



        

        // === Leave Management ===
        SideNavItem leaveManagementSection = new SideNavItem("Leave Management");
        leaveManagementSection.setPrefixComponent(VaadinIcon.CALENDAR.create());
        Tooltip tipLeaveMgmt = Tooltip.forComponent(leaveManagementSection);
        tipLeaveMgmt.setText("Submit, review, and manage employee leave requests");
        tipLeaveMgmt.setPosition(TooltipPosition.END);
        tipLeaveMgmt.setHideDelay(200);
        
        SideNavItem leaveBalance = new SideNavItem("Leave Balances", org.halocambodia.views.leave_management.EmployeeLeaveBalanceView.class, VaadinIcon.CHART.create());
        Tooltip tipLeaveBalance = Tooltip.forComponent(leaveBalance);
        tipLeaveBalance.setText("Manage employee leave balances and entitlements | គ្រប់គ្រងសមតុល្យច្បាប់ប្រចាំឆ្នាំរបស់បុគ្គលិក");
        tipLeaveBalance.setPosition(TooltipPosition.END);
        tipLeaveBalance.setHideDelay(200);
        leaveManagementSection.addItem(leaveBalance);

        SideNavItem leaveRequest = new SideNavItem("Leave Request", LeaveRequestView.class, VaadinIcon.PAPERPLANE.create());
        Tooltip tipLeaveReq = Tooltip.forComponent(leaveRequest);
        tipLeaveReq.setText("Employees submit leave requests for approval");
        tipLeaveReq.setPosition(TooltipPosition.END);
        tipLeaveReq.setHideDelay(200);
        leaveManagementSection.addItem(leaveRequest);
        

        

        SideNavItem supervisorLeave = new SideNavItem("Line Manager Approval", SupervisorLeaveReviewView.class, VaadinIcon.USER_CHECK.create());
        Tooltip tipSupLeave = Tooltip.forComponent(supervisorLeave);
        tipSupLeave.setText("Line Manager and approve leave requests");
        tipSupLeave.setPosition(TooltipPosition.END);
        tipSupLeave.setHideDelay(200);
        leaveManagementSection.addItem(supervisorLeave);


        SideNavItem finalLeave = new SideNavItem("HR Review", FinalLeaveReviewView.class, VaadinIcon.CLIPBOARD_CHECK.create());
        Tooltip tipFinalLeave = Tooltip.forComponent(finalLeave);
        tipFinalLeave.setText("HR reviews and finalizes leave approvals");
        tipFinalLeave.setPosition(TooltipPosition.END);
        tipFinalLeave.setHideDelay(200);
        leaveManagementSection.addItem(finalLeave);


        SideNavItem leaveDocs = new SideNavItem("Leave Summary", LeaveSupportingDocumentView.class, VaadinIcon.PAPERCLIP.create());
        Tooltip tipLeaveDocs = Tooltip.forComponent(leaveDocs);
        tipLeaveDocs.setText("View employee leave history and past requests");
        tipLeaveDocs.setPosition(TooltipPosition.END);
        tipLeaveDocs.setHideDelay(200);
        leaveManagementSection.addItem(leaveDocs);
        nav.addItem(leaveManagementSection);


        // === Attendance Management ===
        SideNavItem attendanceManagementSection = new SideNavItem("Attendance Management");
        attendanceManagementSection.setPrefixComponent(VaadinIcon.CALENDAR_CLOCK.create());
        addTooltip(attendanceManagementSection,"Attendance entry, QC verification, HR final review, and summary "  + "| បញ្ចូលវត្តមាន ផ្ទៀងផ្ទាត់ដោយ QC ពិនិត្យចុងក្រោយដោយ HR និងមើលសេចក្តីសង្ខេប");
        nav.addItem(attendanceManagementSection);

        if (authenticatedUser.hasPage(DailyAttendanceView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem dailyAttendance = new SideNavItem("Daily Attendance", DailyAttendanceView.class, VaadinIcon.TIME_FORWARD.create());
            addTooltip(dailyAttendance, "Create, edit, and submit draft attendance " + "| បង្កើត កែសម្រួល និងបញ្ជូនវត្តមានព្រាង");
            attendanceManagementSection.addItem(dailyAttendance);
        }
        
        if (authenticatedUser.hasPage(AttendanceVerificationView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem attendanceVerification = new SideNavItem("QC Attendance Verification", AttendanceVerificationView.class, VaadinIcon.USER_CHECK.create());
            addTooltip(attendanceVerification,"HR Operations verifies submitted attendance or returns it for correction " + "| ប្រតិបត្តិការ HR ផ្ទៀងផ្ទាត់វត្តមានដែលបានបញ្ជូន ឬបញ្ជូនត្រឡប់ដើម្បីកែតម្រូវ");
            attendanceManagementSection.addItem(attendanceVerification);
        }

        if (authenticatedUser.hasPage(AttendanceFinalReviewView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem attendanceFinalReview = new SideNavItem("HR Attendance Final Review", AttendanceFinalReviewView.class,VaadinIcon.TASKS.create());
            addTooltip(attendanceFinalReview,"HR performs the final review after QC verification "  + "| HR ពិនិត្យវត្តមានចុងក្រោយ បន្ទាប់ពី QC បានផ្ទៀងផ្ទាត់");
            attendanceManagementSection.addItem(attendanceFinalReview);
        }
        if (authenticatedUser.hasPage(AttendanceSummaryView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem attendanceSummary = new SideNavItem("Attendance Summary",AttendanceSummaryView.class,VaadinIcon.CLIPBOARD_CHECK.create());
            addTooltip(attendanceSummary,"View employee attendance by date range, location, and team " + "| មើលវត្តមានបុគ្គលិកតាមកាលបរិច្ឆេទ ទីតាំង និងក្រុម");
            attendanceManagementSection.addItem(attendanceSummary);
        }
        
        if (authenticatedUser.hasPage(LeaveTypeView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem leaveTypes = new SideNavItem( "Duty / Leave", LeaveTypeView.class, VaadinIcon.LIST.create());
            addTooltip(leaveTypes,"Maintain Duty / Leave and Sub Leave | គ្រប់គ្រងកាតព្វកិច្ច / ច្បាប់ និងប្រភេទរង");
            attendanceManagementSection.addItem(leaveTypes);
        }

      
       
        // === Payroll Management ===
        if (authenticatedUser.hasPage(PayrollView.class, AccessPageType.SELECTED_PAGE)) {
            SideNavItem payrollManagement = new SideNavItem("Payroll Management");
            payrollManagement.setPrefixComponent(VaadinIcon.MONEY.create());
            addTooltip(payrollManagement,
                    "Payroll periods, attendance validation, processing and payroll configuration "
                            + "| រយៈពេលប្រាក់បៀវត្ស ការផ្ទៀងផ្ទាត់វត្តមាន ដំណើរការ និងការកំណត់ប្រាក់បៀវត្ស");

            SideNavItem payrollProcessing = new SideNavItem(
                    "Payroll Operations", PayrollView.class, VaadinIcon.MONEY.create());
            addTooltip(payrollProcessing,"Manage payroll periods, attendance locking, calculation and payments "
                    + "| គ្រប់គ្រងរយៈពេល ចាក់សោវត្តមាន គណនា និងបើកប្រាក់បៀវត្ស");


            SideNavItem payrollEmployeeAdjustments = new SideNavItem("Recurring & Loans", PayrollEmployeeAdjustmentsView.class, VaadinIcon.MONEY.create());
            addTooltip(payrollEmployeeAdjustments, "Maintain recurring fixed income, other deductions, company loans, asset damage/loss and other employee recoveries " + "| គ្រប់គ្រងចំណូលប្រចាំ ការកាត់ផ្សេងៗ ប្រាក់កម្ចី ការខូច/បាត់ទ្រព្យសម្បត្តិ និងការសងសំណងបុគ្គលិក");
            

            SideNavItem payrollComponents = new SideNavItem(
                    "Payroll Components", PayrollComponentView.class, VaadinIcon.COG.create());
            addTooltip(payrollComponents,
                    "Maintain earnings, deductions and payroll components used by calculation rules");

            SideNavItem payrollRules = new SideNavItem(
                    "Payroll Rules", PayrollRuleView.class, VaadinIcon.TASKS.create());
            addTooltip(payrollRules,
                    "Maintain yearly attendance, leave, overtime and allowance calculation rules");

            SideNavItem payrollNssfRules = new SideNavItem(
                    "NSSF Rules", PayrollNssfRuleView.class, VaadinIcon.MONEY.create());
            addTooltip(payrollNssfRules,
                    "Maintain effective NSSF healthcare, occupational-risk and pension contribution rules "
                            + "| គ្រប់គ្រងច្បាប់ភាគទាន ប.ស.ស. សម្រាប់សុខភាព ហានិភ័យការងារ និងសោធន");

            SideNavItem payrollTaxRates = new SideNavItem(
                    "Salary Tax Rules", PayrollTaxRateView.class, VaadinIcon.MONEY.create());
            addTooltip(payrollTaxRates,
                    "Maintain Cambodia salary-tax brackets and dependant allowance by year");

            SideNavItem payrollSeniorityRules = new SideNavItem(
                    "Seniority Payment",
                    PayrollSeniorityRuleView.class, VaadinIcon.CLOCK.create());
            addTooltip(payrollSeniorityRules,
                    "Maintain effective UDC seniority rules and inspect June/December calculation results "
                            + "| គ្រប់គ្រងច្បាប់ និងលទ្ធផលប្រាក់បំណាច់អតីតភាព");

            SideNavItem payrollPaymentSettings = new SideNavItem(
                    "Payment Settings",
                    PayrollPaymentSettingView.class, VaadinIcon.CALENDAR.create());
            addTooltip(payrollPaymentSettings,  "Configure one company payment rule: all monthly, one Shift semi-monthly exception, or all semi-monthly");

            payrollManagement.addItem(payrollProcessing);
           // payrollManagement.addItem(payrollData);
            payrollManagement.addItem(payrollEmployeeAdjustments);
            payrollManagement.addItem(payrollComponents);
            payrollManagement.addItem(payrollRules);
            payrollManagement.addItem(payrollNssfRules);
            payrollManagement.addItem(payrollTaxRates);
            payrollManagement.addItem(payrollSeniorityRules);
            payrollManagement.addItem(payrollPaymentSettings);
            nav.addItem(payrollManagement);
        }
       

       
       // === Policy Management ===
       SideNavItem policyManagementSection = new SideNavItem("Policy Management");
       policyManagementSection.setPrefixComponent(VaadinIcon.FILE_TREE.create());
       addTooltip(policyManagementSection,"Manage policies, categories, acknowledgements, history, and access logs");
       nav.addItem(policyManagementSection);

       // HR / Policy administrators: policy master data and CRUD.
       SideNavItem policies = new SideNavItem("Manage Policies", PolicyView.class,VaadinIcon.FILE_TEXT.create());
       addTooltip(policies,"Create, edit, publish, and manage policy files");
       policyManagementSection.addItem(policies);

       SideNavItem policyCategories = new SideNavItem("Policy Categories",PolicyCategoryView.class,VaadinIcon.FOLDER.create());
       addTooltip(policyCategories,"Manage policy categories such as HR, Finance, ICT, and Safety");
       policyManagementSection.addItem(policyCategories);

       // Employees: list available policies and view acknowledgement status.
       // PolicyReadView is not added directly because it requires a publicToken:
       // /policy/read/{publicToken}
       SideNavItem myAcknowledgements = new SideNavItem("My Acknowledgements",PolicyAcknowledgeView.class,VaadinIcon.CHECK_CIRCLE.create());
       addTooltip(myAcknowledgements,"Open available policies and review your acknowledgement status");
       policyManagementSection.addItem(myAcknowledgements);

/*
       SideNavItem policyAccessLogs = new SideNavItem("Access Logs",PolicyAccessLogView.class,VaadinIcon.EYE.create());
       addTooltip(policyAccessLogs,"Review policy views, prints, downloads, and acknowledgements");
       policyManagementSection.addItem(policyAccessLogs);
       */
       
       if (authenticatedUser.hasPage(PolicyAccessLogView.class, AccessPageType.SELECTED_PAGE)) {
    	    SideNavItem policyAccessLogs = new SideNavItem("Access Logs", PolicyAccessLogView.class,VaadinIcon.EYE.create());
    	    addTooltip( policyAccessLogs,"Review policy views, prints, downloads, and acknowledgements");
    	    policyManagementSection.addItem(policyAccessLogs );
    	}
       
        

        // === Reports ===
        SideNavItem reportSection = new SideNavItem("Reports");
        reportSection.setPrefixComponent(VaadinIcon.CHART.create());
        Tooltip tipReports = Tooltip.forComponent(reportSection);
        tipReports.setText("Generate analytical and operational reports");
        tipReports.setPosition(TooltipPosition.END);
        tipReports.setHideDelay(200);

        SideNavItem simpleReports = new SideNavItem("Simple reports", ReportsView.class, VaadinIcon.FILE_TEXT.create());
        Tooltip tipSimple = Tooltip.forComponent(simpleReports);
        tipSimple.setText("View predefined simple HR reports");
        tipSimple.setPosition(TooltipPosition.END);
        tipSimple.setHideDelay(200);
        reportSection.addItem(simpleReports);

        SideNavItem dataExplorer = new SideNavItem("Data Explorer", DataExplorerView.class, VaadinIcon.BAR_CHART.create());
        Tooltip tipExplorer = Tooltip.forComponent(dataExplorer);
        tipExplorer.setText("Explore and analyze HR data interactively");
        tipExplorer.setPosition(TooltipPosition.END);
        tipExplorer.setHideDelay(200);
        reportSection.addItem(dataExplorer);
        nav.addItem(reportSection);

 

        // === Settings ===
        SideNavItem settingSection = new SideNavItem("Settings");
        settingSection.setPrefixComponent(VaadinIcon.COG.create());
        Tooltip tipSettings = Tooltip.forComponent(settingSection);
        tipSettings.setText("Configure system preferences and HR settings");
        tipSettings.setPosition(TooltipPosition.END);
        tipSettings.setHideDelay(200);
        nav.addItem(settingSection);
        
        SideNavItem organisationSection = new SideNavItem("Organisation");
        organisationSection.setPrefixComponent(VaadinIcon.BUILDING.create());
        Tooltip tipOrganisationSection = Tooltip.forComponent(organisationSection);
        tipOrganisationSection.setText("Organisation structure and reference lists");
        tipOrganisationSection.setPosition(TooltipPosition.END);
        tipOrganisationSection.setHideDelay(200);
        settingSection.addItem(organisationSection);
        
        SideNavItem branchSetting = new SideNavItem("Locations", org.halocambodia.views.branch.BranchView.class, VaadinIcon.BUILDING_O.create());
        Tooltip tipBranchSetting = Tooltip.forComponent(branchSetting);
        tipBranchSetting.setText("Manage location master data");
        tipBranchSetting.setPosition(TooltipPosition.END);
        tipBranchSetting.setHideDelay(200);
        organisationSection.addItem(branchSetting);
        
        
        SideNavItem departmentSetting = new SideNavItem("Department", DepartmentView.class, VaadinIcon.OFFICE.create());
        Tooltip tipdepartmentSettingSetting = Tooltip.forComponent(departmentSetting);
        tipdepartmentSettingSetting.setText("Manage department master data");
        tipdepartmentSettingSetting.setPosition(TooltipPosition.END);
        tipdepartmentSettingSetting.setHideDelay(200);       
        organisationSection.addItem(departmentSetting);
        
        SideNavItem teamSetting = new SideNavItem("Teams", TeamsView.class, VaadinIcon.GROUP.create());
        Tooltip tipTeamSettingSetting = Tooltip.forComponent(teamSetting);
        tipTeamSettingSetting.setText("Manage team master data");
        tipTeamSettingSetting.setPosition(TooltipPosition.END);
        tipTeamSettingSetting.setHideDelay(200);       
        organisationSection.addItem(teamSetting);
        
        SideNavItem positionSetting = new SideNavItem("Position", PositionView.class, VaadinIcon.USER_STAR.create());
        Tooltip tipPositionSetting = Tooltip.forComponent(positionSetting);
        tipPositionSetting.setText("Manage position master data");
        tipPositionSetting.setPosition(TooltipPosition.END);
        tipPositionSetting.setHideDelay(200);
        organisationSection.addItem(positionSetting);
        
        SideNavItem poGradeSetting = new SideNavItem("Salary Grade", PoGradeView.class, VaadinIcon.LIST_OL.create());

        Tooltip tipPoGradeSetting = Tooltip.forComponent(poGradeSetting);
        tipPoGradeSetting.setText("Manage Salary Grade master data");
        tipPoGradeSetting.setPosition(TooltipPosition.END);
        tipPoGradeSetting.setHideDelay(200);
        organisationSection.addItem(poGradeSetting); 
        
        SideNavItem donorSetting = new SideNavItem("Donors", DonorView.class, VaadinIcon.DOLLAR.create());
        Tooltip tipDonorSetting = Tooltip.forComponent(donorSetting);
        tipDonorSetting.setText("Manage donor master data | គ្រប់គ្រងទិន្នន័យអ្នកឧបត្ថម្ភ");
        tipDonorSetting.setPosition(TooltipPosition.END);
        tipDonorSetting.setHideDelay(200);
        organisationSection.addItem(donorSetting);
        
        SideNavItem contractSetting = new SideNavItem("Contracts", ContractsView.class, VaadinIcon.FILE_TEXT.create());

        Tooltip tipContract = Tooltip.forComponent(contractSetting);
        tipContract.setText("Manage contracts linked to donors | គ្រប់គ្រងកិច្ចសន្យា");
        tipContract.setPosition(TooltipPosition.END);
        tipContract.setHideDelay(200);

        organisationSection.addItem(contractSetting);
        
        
        
        SideNavItem disciplinaryActionsSetting = new SideNavItem("Disciplinary Actions", DisciplinaryView.class, VaadinIcon.GAVEL.create());
        Tooltip tipDisciplinaryActionsSetting = Tooltip.forComponent(disciplinaryActionsSetting);
        tipDisciplinaryActionsSetting.setText("Manage disciplinary actions master data");
        tipDisciplinaryActionsSetting.setPosition(TooltipPosition.END);
        tipDisciplinaryActionsSetting.setHideDelay(200);
        organisationSection.addItem(disciplinaryActionsSetting);

        SideNavItem careerTypeSetting =new SideNavItem("Career Types", CareerTypeView.class, VaadinIcon.CHART_GRID.create());
        Tooltip tipCareerTypeSetting = Tooltip.forComponent(careerTypeSetting);
        tipCareerTypeSetting.setText("Manage career types master data");
        tipCareerTypeSetting.setPosition(TooltipPosition.END);
        tipCareerTypeSetting.setHideDelay(200);
        organisationSection.addItem(careerTypeSetting);
 
        
        SideNavItem peopleHRListSection = new SideNavItem("People & HR Lists");        
        peopleHRListSection.setPrefixComponent(VaadinIcon.USER.create());
        Tooltip tipPeopleHRListSection = Tooltip.forComponent(peopleHRListSection);
        tipPeopleHRListSection.setText("Reference lists for people records");
        tipPeopleHRListSection.setPosition(TooltipPosition.END);
        tipPeopleHRListSection.setHideDelay(200);
        settingSection.addItem(peopleHRListSection);
        
        SideNavItem testList = new SideNavItem("Gazetteer", GazetteerView.class, VaadinIcon.MAP_MARKER.create());
        Tooltip tipTestList = Tooltip.forComponent(testList);
        tipTestList.setText("Gazetteer / locations reference");
        tipTestList.setPosition(TooltipPosition.END);
        tipTestList.setHideDelay(200);
        peopleHRListSection.addItem(testList);

        
        
        SideNavItem relationshipsSetting = new SideNavItem("Relationships", RelationshipView.class, VaadinIcon.HEART.create());
        Tooltip tiprelationshipsSetting = Tooltip.forComponent(relationshipsSetting);
        tiprelationshipsSetting.setText("Manage relationship master data");
        tiprelationshipsSetting.setPosition(TooltipPosition.END);
        tiprelationshipsSetting.setHideDelay(200);       
        peopleHRListSection.addItem(relationshipsSetting);
        
        SideNavItem LanguagesSetting = new SideNavItem("Languages", LanguageView.class, VaadinIcon.COMMENT.create());
        Tooltip tipLanguagesSetting = Tooltip.forComponent(LanguagesSetting);
        tipLanguagesSetting.setText("Manage language master data");
        tipLanguagesSetting.setPosition(TooltipPosition.END);
        tipLanguagesSetting.setHideDelay(200);       
        peopleHRListSection.addItem(LanguagesSetting);
        
        SideNavItem DrivingLicenseSetting = new SideNavItem("Driving License", DrivingLicenseView.class, VaadinIcon.CAR.create());
        Tooltip tipDrivingLicenseSetting = Tooltip.forComponent(DrivingLicenseSetting);
        tipDrivingLicenseSetting.setText("Manage driving license master data");
        tipDrivingLicenseSetting.setPosition(TooltipPosition.END);
        tipDrivingLicenseSetting.setHideDelay(200);       
        peopleHRListSection.addItem(DrivingLicenseSetting);
        
        SideNavItem attachmentTypesSetting =new SideNavItem("Attachment Types", AttachmentTypeView.class, VaadinIcon.PAPERCLIP.create());
        Tooltip tipAttachmentTypesSetting = Tooltip.forComponent(attachmentTypesSetting);
        tipAttachmentTypesSetting.setText("Manage document attachment type master data");
        tipAttachmentTypesSetting.setPosition(TooltipPosition.END);
        tipAttachmentTypesSetting.setHideDelay(200);
        peopleHRListSection.addItem(attachmentTypesSetting);
        
        SideNavItem religionsSetting = new SideNavItem("Religions", ReligionsView.class, VaadinIcon.GLOBE.create());
        Tooltip tipReligionsSetting = Tooltip.forComponent(religionsSetting);
        tipReligionsSetting.setText("Manage religion master data");
        tipReligionsSetting.setPosition(TooltipPosition.END);
        tipReligionsSetting.setHideDelay(200);
        peopleHRListSection.addItem(religionsSetting);
        
        
        
        
        SideNavItem educationSection = new SideNavItem("Education");
        educationSection.setPrefixComponent(VaadinIcon.ACADEMY_CAP.create());
        Tooltip tipEducationSection = Tooltip.forComponent(educationSection);
        tipEducationSection.setText("Education master data");
        tipEducationSection.setPosition(TooltipPosition.END);
        tipEducationSection.setHideDelay(200);
        settingSection.addItem(educationSection);
        
        SideNavItem educationQualificationsSetting = new SideNavItem("Education Qualifications", EducationQualificationsView.class, VaadinIcon.DIPLOMA_SCROLL.create());
        Tooltip tipeducationQualificationsSetting = Tooltip.forComponent(educationQualificationsSetting);
        tipeducationQualificationsSetting.setText("Manage qualification master data");
        tipeducationQualificationsSetting.setPosition(TooltipPosition.END);
        tipeducationQualificationsSetting.setHideDelay(200);       
        educationSection.addItem(educationQualificationsSetting);
        
        SideNavItem educationCentersSetting = new SideNavItem("Education Centers", EducationCenterView.class, VaadinIcon.INSTITUTION.create());
        Tooltip tipEducationCentersSettingSetting = Tooltip.forComponent(educationCentersSetting);
        tipEducationCentersSettingSetting.setText("Manage education center master data");
        tipEducationCentersSettingSetting.setPosition(TooltipPosition.END);
        tipEducationCentersSettingSetting.setHideDelay(200);       
        educationSection.addItem(educationCentersSetting);
        
        
        SideNavItem wellbeingSection = new SideNavItem("Wellbeing");
        wellbeingSection.setPrefixComponent(VaadinIcon.HEART.create());
        Tooltip tipWellbeingSection = Tooltip.forComponent(wellbeingSection);
        tipWellbeingSection.setText("Wellbeing and health reference lists");
        tipWellbeingSection.setPosition(TooltipPosition.END);
        tipWellbeingSection.setHideDelay(200);
        settingSection.addItem(wellbeingSection);
        
        
        SideNavItem disabilityTypesSetting = new SideNavItem("Disability Types", DisabilityTypeView.class, VaadinIcon.ACCESSIBILITY.create());
        Tooltip tipDisabilityTypesSetting = Tooltip.forComponent(disabilityTypesSetting);
        tipDisabilityTypesSetting.setText("Manage disability types master data");
        tipDisabilityTypesSetting.setPosition(TooltipPosition.END);
        tipDisabilityTypesSetting.setHideDelay(200);       
        wellbeingSection.addItem(disabilityTypesSetting);
        
        SideNavItem disabilityTypeOptionVSetting = new SideNavItem("Disability Options", DisabilityTypeOptionView.class, VaadinIcon.LIST.create());
        Tooltip tipDisabilityTypeOptionVSetting = Tooltip.forComponent(disabilityTypeOptionVSetting);
        tipDisabilityTypeOptionVSetting.setText("Manage disability options master data");
        tipDisabilityTypeOptionVSetting.setPosition(TooltipPosition.END);
        tipDisabilityTypeOptionVSetting.setHideDelay(200);       
        wellbeingSection.addItem(disabilityTypeOptionVSetting);
        
        
        SideNavItem vaccinationsSetting = new SideNavItem("Vaccinations", VaccinationView.class, VaadinIcon.MEDAL.create());
        Tooltip tipVaccinationsSetting = Tooltip.forComponent(vaccinationsSetting);
        tipVaccinationsSetting.setText("Manage vaccination master data");
        tipVaccinationsSetting.setPosition(TooltipPosition.END);
        tipVaccinationsSetting.setHideDelay(200);       
        wellbeingSection.addItem(vaccinationsSetting);
        
        SideNavItem vaccinationTypeSetting = new SideNavItem("Vaccination Types", VaccinationTypeView.class, VaadinIcon.TAGS.create());
        Tooltip tipVaccinationTypeSetting = Tooltip.forComponent(vaccinationTypeSetting);
        tipVaccinationTypeSetting.setText("Manage vaccination type master data");
        tipVaccinationTypeSetting.setPosition(TooltipPosition.END);
        tipVaccinationTypeSetting.setHideDelay(200);       
        wellbeingSection.addItem(vaccinationTypeSetting);
        

        // === IM/ICT ===
        SideNavItem imIctSection = new SideNavItem("Admin");
        imIctSection.setPrefixComponent(VaadinIcon.SERVER.create());
        Tooltip tipImict = Tooltip.forComponent(imIctSection);
        tipImict.setText("Manage system users, roles, and data integrations");
        tipImict.setPosition(TooltipPosition.END);
        tipImict.setHideDelay(200);

        SideNavItem userList = new SideNavItem("Users", UserManagementView.class, VaadinIcon.GROUP.create());
        Tooltip tipUserList = Tooltip.forComponent(userList);
        tipUserList.setText("View and manage all system users");
        tipUserList.setPosition(TooltipPosition.END);
        tipUserList.setHideDelay(200);
        imIctSection.addItem(userList);

        SideNavItem userRoles = new SideNavItem("Permissions", RoleManagementView.class, VaadinIcon.KEY.create());
        Tooltip tipUserRoles = Tooltip.forComponent(userRoles);
        tipUserRoles.setText("Manage access roles and permissions");
        tipUserRoles.setPosition(TooltipPosition.END);
        tipUserRoles.setHideDelay(200);
        imIctSection.addItem(userRoles);

        SideNavItem dataFeeds = new SideNavItem("Data Feeds", DataFeedView.class, VaadinIcon.REFRESH.create());
        Tooltip tipDataFeeds = Tooltip.forComponent(dataFeeds);
        tipDataFeeds.setText("Monitor automated data synchronization and logs");
        tipDataFeeds.setPosition(TooltipPosition.END);
        tipDataFeeds.setHideDelay(200);
        imIctSection.addItem(dataFeeds);
        nav.addItem(imIctSection);
        

        return nav;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (viewTitle != null) {
            viewTitle.setText(getCurrentPageTitle());
        }
    }


    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
    
    private void addTooltip(Component component, String text) {
        Tooltip tooltip = Tooltip.forComponent(component);
        tooltip.setText(text);
        tooltip.setPosition(TooltipPosition.END);
        tooltip.setHideDelay(200);
    }


}
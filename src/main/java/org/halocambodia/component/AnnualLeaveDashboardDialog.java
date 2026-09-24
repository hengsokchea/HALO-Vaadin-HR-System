package org.halocambodia.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;

import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeLeave;
import org.halocambodia.data.EmployeeLeaveBalance;
import org.halocambodia.data.EmployeeLeaveBalanceRepository;
import org.halocambodia.data.EmployeeLeaveDetail;
import org.halocambodia.data.EmployeeLeaveRepository;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.User;
import org.halocambodia.views.CustomDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class AnnualLeaveDashboardDialog extends CustomDialog {

	 private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;
     private List<EmployeeLeaveBalance> balances;

    
    public AnnualLeaveDashboardDialog(EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository) {
    	super("My Leave Dashboard | ផ្ទាំងច្បាប់របស់ខ្ញុំ");
        this.employeeLeaveBalanceRepository = employeeLeaveBalanceRepository;

    }

    public void show(User user) {
        // Clear existing content before adding new
        removeAll();
        
        configureDialog();
        
        VerticalLayout mainContent = createMainContent(user);
        add(mainContent);
        
        
        open();
    }

    private void configureDialog() {
       // setHeaderTitle("My Annual Leave Dashboard | ផ្ទាំងគ្រប់គ្រងច្បាប់ប្រចាំឆ្នាំរបស់ខ្ញុំ");
        setDraggable(true);
        setResizable(true);
       // setWidth("900px");
        //setWidth("95vw");
        //setMaxWidth("900px");
    }

    private VerticalLayout createMainContent(User user) {
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);
        mainContent.setWidthFull();
        
        int year = java.time.Year.now().getValue();
        balances = employeeLeaveBalanceRepository.findByEmployeeInsuranceNoAndYearAndLeaveType(Integer.valueOf(user.getInsurance()), year, 1L);
        
        if (balances.isEmpty()) {
            mainContent.add(createEmptyState(year));
        } else {
            EmployeeLeaveBalance elb = balances.get(0);
            
            double total = elb.getTotalDays() != null ? elb.getTotalDays().doubleValue() : 0;
            double used = elb.getTakenDays() != null ? elb.getTakenDays().doubleValue() : 0;
            double remaining = elb.getRemainingDaysCalc() != null ? elb.getRemainingDaysCalc().doubleValue() : 0;
            double pending = elb.getPendingDays() != null ? elb.getPendingDays().doubleValue() : 0;
            double available = elb.getAvailableDays() != null ? elb.getAvailableDays().doubleValue() : 0;
            
            // Year Header
            mainContent.add(createYearHeader(year));
            
            // Stats Cards Grid
            mainContent.add(createStatsGrid(total, used, remaining, pending, available));
            
            // Progress Section
            mainContent.add(createProgressSection(total, used, pending, remaining, available));
            
            mainContent.add(createRecentLeaveRequests(user));
        }
        
        return mainContent;
    }
    
    private com.vaadin.flow.component.Component createRecentLeaveRequests(User user) {

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        Span title = new Span(
                "Recent Leave Requests | សំណើសុំច្បាប់ថ្មីៗ");
        title.getStyle()
                .set("font-size", "18px")
                .set("font-weight", "600");

        Grid<EmployeeLeave> grid = new Grid<>();

        grid.addColumn(EmployeeLeave::getRequestDate)
                .setHeader("Request Date")
                .setAutoWidth(true)
                .setResizable(true)
                .setComparator(Comparator.comparing(EmployeeLeave::getRequestDate, 
                    Comparator.nullsLast(Comparator.reverseOrder())));

        // Combine multiple leave types
        grid.addColumn(leave -> {
            if (leave.getEmployeeLeaveDetails() == null || leave.getEmployeeLeaveDetails().isEmpty()) {
                return "";
            }
            return leave.getEmployeeLeaveDetails().stream()
                .map(detail -> detail.getLeaveType() != null ? detail.getLeaveType().getLeaveNameEn() : "")
                .filter(s -> !s.isEmpty())
                .distinct()
                .reduce((s1, s2) -> s1 + ", " + s2)
                .orElse("");
        }).setHeader("Leave Type(s)").setAutoWidth(true).setResizable(true);

        // Combine multiple date ranges
        grid.addColumn(leave -> {
            if (leave.getEmployeeLeaveDetails() == null || leave.getEmployeeLeaveDetails().isEmpty()) {
                return "";
            }
            return leave.getEmployeeLeaveDetails().stream()
                .map(EmployeeLeaveDetail::getDateRangeDisplay)
                .filter(s -> s != null && !s.isEmpty())
                .reduce((s1, s2) -> s1 + "; " + s2)
                .orElse("");
        }).setHeader("Date Range(s)").setAutoWidth(true).setResizable(true);

        // Combine total days
        grid.addColumn(leave -> {
            if (leave.getEmployeeLeaveDetails() == null || leave.getEmployeeLeaveDetails().isEmpty()) {
                return "";
            }
            double totalDays = leave.getEmployeeLeaveDetails().stream()
                .mapToDouble(detail -> detail.getNumberOfDay() != null ? detail.getNumberOfDay().doubleValue() : 0.0)
                .sum();
            return String.format("%.1f", totalDays);
        }).setHeader("Total Days").setAutoWidth(true).setResizable(true);

        grid.addColumn(leave -> leave.getLineManagerCheckedStatus() != null 
                ? leave.getLineManagerCheckedStatus().getLeaveStatusName() : "")
                .setHeader("Line Manager Status").setAutoWidth(true).setResizable(true);
                
        grid.addColumn(leave -> leave.getHrVerificationStatus() != null 
                ? leave.getHrVerificationStatus().getLeaveStatusName() : "")
                .setHeader("HR Status").setAutoWidth(true).setResizable(true);

        grid.setAllRowsVisible(true);
        grid.setWidthFull();

        // Get current year
        int currentYear = java.time.Year.now().getValue();
        
        List<EmployeeLeave> recentRequests = balances.getFirst().getEmployee().getEmployeeLeaves();
        
        // Filter leave requests by current year based on request date
        List<EmployeeLeave> filteredRequests = recentRequests.stream()
            .filter(leave -> {
                if (leave.getRequestDate() == null) {
                    return false;
                }
                // Extract year from request date and compare with current year
                java.time.LocalDate requestDate = leave.getRequestDate();
                return requestDate.getYear() == currentYear;
            })
            .collect(java.util.stream.Collectors.toList());
        
        // Sort by request date descending
        filteredRequests.sort(Comparator.comparing(EmployeeLeave::getRequestDate, 
            Comparator.nullsLast(Comparator.reverseOrder())));
        
        grid.setItems(filteredRequests);
        
        // Optional: Show a message if no requests in current year
        if (filteredRequests.isEmpty()) {
            layout.add(new Span("No leave requests found for " + currentYear + 
                " | គ្មានសំណើសុំច្បាប់សម្រាប់ឆ្នាំ " + currentYear));
        }

        layout.add(title, grid);

        return layout;
    }

    private com.vaadin.flow.component.Component createYearHeader(int year) {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        header.setPadding(true);
        
        Div yearBadge = new Div();
        yearBadge.getStyle()
            .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
            .set("border-radius", "20px")
            .set("padding", "8px 24px")
            .set("color", "white")
            .set("font-weight", "bold")
            .set("font-size", "16px")
            .set("text-align", "center");
        
        Icon calendarIcon = VaadinIcon.CALENDAR.create();
        calendarIcon.setSize("18px");
        calendarIcon.getStyle().set("margin-right", "8px");
        
        Span yearText = new Span("Annual Leave Summary - Year \n សង្ខេបច្បាប់ប្រចាំឆ្នាំ " + year);
        yearText.getStyle().set("vertical-align", "middle");
        
        yearBadge.add(calendarIcon, yearText);
        header.add(yearBadge);
        
        return header;
    }

    private Div createStatsGrid(double total, double used, double remaining,  double pending, double available) {
    	
    	Div grid = new Div();
        grid.getStyle()
            .set("display", "grid")
            .set("grid-template-columns",
                 "repeat(auto-fit, minmax(140px, 1fr))")
            .set("gap", "12px")
            .set("width", "100%");

        
        grid.add(createStatCard(
            "Total Leave",
            String.format("%.1f", total),
            "days",
            "សរុបច្បាប់ប្រចាំឆ្នាំ",
            "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            VaadinIcon.CALENDAR.create()
        ));
        
        grid.add(createStatCard(
            "Used Leave",
            String.format("%.1f", used),
            "days",
            "បានប្រើរួច",
            "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
            VaadinIcon.CHECK_SQUARE.create()
        ));
        
        grid.add(createStatCard(
            "Remaining",
            String.format("%.1f", remaining),
            "days",
            "នៅសល់", 
             "linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%)",
            VaadinIcon.CALENDAR_CLOCK.create()
        ));
        
        grid.add(createStatCard(
            "Pending",
            String.format("%.1f", pending),
            "days",
            "កំពុងរងចាំ",
            "linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)",
            VaadinIcon.CLOCK.create()
        ));
        
        grid.add(createStatCard(
            "Available",
            String.format("%.1f", available),
            "days",
            "អាចប្រើបានភ្លាមៗ",
            "linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)",
            VaadinIcon.CHECK.create()
        ));
        
        return grid;
    }

    private Div createStatCard(String title, String value, String unit, String subtitle, 
            String gradient, Icon icon) {
        Div card = new Div();
        card.getStyle()
            .set("background", gradient)
            .set("border-radius", "12px")
            .set("padding", "16px")
            .set("flex", "1")
            .set("position", "relative")
            .set("overflow", "hidden")
            .set("min-width", "0");

        // Determine text color based on gradient brightness
        boolean isLightCard = gradient.contains("#ffecd2") || gradient.contains("#84fab0") ||gradient.contains("#fcb69f") || gradient.contains("#a8edea") || gradient.contains("#fed6e3");

        String textColor = "white"; // isLightCard ? "#0082B3" : "white";
        String shadowColor = isLightCard ? "none" : "0 1px 2px rgba(0,0,0,0.1)";
        String valueShadow = isLightCard ? "none" : "0 2px 4px rgba(0,0,0,0.2)";

        // Background icon
        Icon bgIcon = icon;
        bgIcon.setSize("60px");
        bgIcon.getStyle()
            .set("position", "absolute")
            .set("right", "-5px")
            .set("bottom", "-5px")
            .set("opacity", "0.15");

        // Title
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
            .set("font-size", "16px")
            .set("font-weight", "600")
            .set("display", "block")
            .set("color", textColor)
            .set("text-shadow", shadowColor);

        // Value
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
            .set("font-size", "32px")
            .set("font-weight", "bold")
            .set("display", "block")
            .set("margin", "8px 0 4px 0")
            .set("color", textColor)
            .set("text-shadow", valueShadow);

        Span unitSpan = new Span(unit);
        unitSpan.getStyle()
            .set("font-size", "14px")
            .set("color", textColor)
            .set("font-weight", "500");

        // Subtitle
        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.getStyle()
            .set("font-size", "14px")
            .set("color", textColor)
            .set("display", "block")
            .set("margin-top", "6px")
            .set("font-weight", "500");

        HorizontalLayout valueLayout = new HorizontalLayout();
        valueLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        valueLayout.setSpacing(true);
        valueLayout.add(valueSpan, unitSpan);

        card.add(bgIcon, titleSpan, valueLayout, subtitleSpan);
        return card;
    }

    private com.vaadin.flow.component.Component createProgressSection(double total, double used, double pending, double remaining,double available) {
        VerticalLayout progressSection = new VerticalLayout();
        progressSection.setSpacing(true);
        progressSection.setPadding(false);
        progressSection.setWidthFull();
        
        // Calculate percentage
        double usedValue = used + pending;
        double percentage = total > 0 ? (usedValue / total) * 100 : 0;
        double remainingPercentage = total > 0 ? (remaining / total) * 100 : 0;
        
        // Create a styled container
        Div progressCard = new Div();
        progressCard.getStyle()
            //.set("background", "#f8f9fa")
            .set("border-radius", "12px")
            //.set("padding", "20px")
            .set("margin", "10px 0");
        
        // Title
        Span title = new Span("Leave Usage Overview | ទិដ្ឋភាពទូទៅនៃការប្រើប្រាស់ច្បាប់");
        title.getStyle()
            .set("font-size", "16px")
            .set("font-weight", "bold")
            .set("display", "block")
            .set("margin-bottom", "15px");
        
        // Progress bar wrapper
        Div progressWrapper = new Div();
        progressWrapper.getStyle()
            .set("position", "relative")
            .set("width", "100%")
            .set("margin", "10px 0");
        
        // Create custom progress bar using Div
        Div progressBg = new Div();
        progressBg.getStyle()
            .set("background", "#e9ecef")
            .set("border-radius", "8px")
            .set("height", "40px")
            .set("width", "100%")
            .set("position", "relative")
            .set("overflow", "hidden");
        
        Div progressFill = new Div();
        progressFill.getStyle()
            .set("background", "linear-gradient(90deg, #667eea 0%, #764ba2 100%)")
            .set("border-radius", "8px")
            .set("height", "100%")
            .set("width", percentage + "%")
            .set("transition", "width 0.5s ease-in-out");
        
        // Percentage text
        Div percentageText = new Div();
        percentageText.setText(String.format("%.1f%%", percentage));
        percentageText.getStyle()
            .set("position", "absolute")
            .set("top", "50%")
            .set("left", "50%")
            .set("transform", "translate(-50%, -50%)")
            .set("font-size", "14px")
            .set("font-weight", "bold")
            .set("color", percentage > 50 ? "white" : "#2c3e50")
            .set("text-shadow", percentage > 50 ? "0 1px 2px rgba(0,0,0,0.3)" : "none")
            .set("z-index", "10")
            .set("pointer-events", "none");
        
        progressBg.add(progressFill);
        progressWrapper.add(progressBg, percentageText);
        
        // Statistics
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        statsLayout.setPadding(false);
        statsLayout.getStyle().set("margin-top", "15px");
        
        Div availableStat = new Div();
        availableStat.add(new Span("Available | អាចប្រើប្រាស់បាន: "), new Span(String.format("%.1f", available) + " days"));
        availableStat.getStyle().set("color","#667eea").set("font-weight", "500");

        
        statsLayout.add( availableStat);
        
        progressCard.add( progressWrapper, statsLayout);
        progressCard.setWidthFull();
        
        return progressCard;
    }

    private Div createEmptyState(int year) {
        Div emptyState = new Div();
        emptyState.getStyle()
            .set("text-align", "center")
            .set("padding", "60px 40px")
            .set("background", "linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%)")
            .set("border-radius", "16px");
        
        Icon icon = VaadinIcon.CALENDAR_USER.create();
        icon.setSize("64px");
        icon.getStyle().set("color", "#667eea");
        
        Span title = new Span("No Annual Leave Balance Found");
        title.getStyle()
            .set("display", "block")
            .set("font-size", "18px")
            .set("font-weight", "bold")
            .set("margin-top", "20px")
            .set("color", "#333");
        
        Span message = new Span("No annual leave records found for the year " + year + 
                ".\nមិនមានទិន្នន័យច្បាប់ប្រចាំឆ្នាំសម្រាប់ឆ្នាំ " + year + 
                " ។\nPlease contact HR for assistance. | សូមទាក់ទងផ្នែកធនធានមនុស្សដើម្បីស្នើសុំជំនួយ។");
        message.getStyle()
            .set("display", "block")
            .set("margin-top", "10px")
            .set("color", "#666")
            .set("font-size", "14px");
        
        emptyState.add(icon, title, message);
        return emptyState;
    }




}
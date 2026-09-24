package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.enums.PayrollAttendanceControlStatus;
import org.halocambodia.views.CustomDialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

final class PayrollViewSupport {

    private PayrollViewSupport() {
    }

    static VerticalLayout page(String title, String description) {

        VerticalLayout page = new VerticalLayout();

        if (title != null && !title.isBlank()) {
            H2 heading = new H2(title);
            heading.addClassName("payroll-page-title");
            page.add(heading);
        }

        if (description != null && !description.isBlank()) {
            Paragraph subtitle = new Paragraph(description);
            subtitle.addClassName("payroll-page-subtitle");
            page.add(subtitle);
        }

        page.setSizeFull();
        page.setPadding(true);
        page.setSpacing(true);
        page.addClassName("payroll-page");

        return page;
    }

    static H3 sectionTitle(String text) {

        H3 title = new H3(text);

        title.addClassName("payroll-section-title");

        return title;

    }

    static Div kpi(
            String title,
            String value,
            String subtitle,
            VaadinIcon icon,
            String variant) {

        Div card = new Div();
        card.addClassName("payroll-kpi-card");

        if (variant != null && !variant.isBlank()) {
            card.addClassName("payroll-kpi-" + variant);
            if ("primary".equals(variant)) {
                card.addClassName("payroll-kpi-card-featured");
            }
        }

        Div header = new Div();
        header.addClassName("payroll-kpi-header");

        Span iconBox = new Span(icon.create());
        iconBox.addClassName("payroll-kpi-icon");

        Span label = new Span(title);
        label.addClassName("payroll-kpi-label");

        header.add(iconBox, label);

        H2 amount = new H2(value);
        amount.addClassName("payroll-kpi-value");

        Span note = new Span(subtitle);
        note.addClassName("payroll-kpi-note");

        card.add(header, amount, note);

        return card;
    }

    static Div readOnly(String label, String value) {

        Div field = new Div();

        field.addClassName("payroll-readonly-field");

        Span fieldLabel = new Span(label);

        fieldLabel.addClassName("payroll-readonly-label");

        Span fieldValue = new Span(nvl(value));

        fieldValue.addClassName("payroll-readonly-value");

        field.add(fieldLabel, fieldValue);

        return field;

    }

    static HorizontalLayout toolbar(Component... components) {

        HorizontalLayout toolbar = new HorizontalLayout(components);

        toolbar.setAlignItems(Alignment.CENTER);

        toolbar.setWrap(true);

        toolbar.setPadding(false);

        toolbar.addClassName("payroll-toolbar");

        return toolbar;

    }

    static FormLayout form(Component... fields) {

        FormLayout form = new FormLayout(fields);

        form.setWidthFull();

        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),

                new FormLayout.ResponsiveStep("700px", 2));

        return form;

    }

    static CustomDialog editorDialog(String title) {

        CustomDialog dialog = new CustomDialog(title);
        dialog.addClassName("payroll-dialog");
        dialog.removeThemeVariants(DialogVariant.NO_PADDING);
        dialog.setWidth("min(850px, calc(100vw - 32px))");
        dialog.setMaxHeight("calc(100dvh - 32px)");

        return dialog;

    }

    static PayrollConfirmDialog confirm(String title, String message) {

        PayrollConfirmDialog confirm = new PayrollConfirmDialog(title, message);
        confirm.setCancelable(true);
        confirm.setCancelText("Cancel | បោះបង់");
        confirm.setConfirmText("Confirm | បញ្ជាក់");
        confirm.setConfirmButtonTheme("primary");

        return confirm;

    }

    static Button primaryButton(String text, VaadinIcon icon) {

        Button button = actionButton(text, icon);

        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return button;

    }
    
    static Button errorButton(String text, VaadinIcon icon) {

        Button button = actionButton(text, icon);

        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_ERROR);

        return button;

    }
    

    static Button actionButton(String text, VaadinIcon icon) {
        return new Button(text, icon.create());
    }

    static <T> void configureGrid(Grid<T> grid) {
        grid.setSizeFull();
        grid.setColumnReorderingAllowed(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT,GridVariant.LUMO_COLUMN_BORDERS);
    }

    static <T> void moneyColumn(Grid<T> grid, String header,
            com.vaadin.flow.function.ValueProvider<T, BigDecimal> provider) {

        Grid.Column<T> column = grid.addColumn(item -> usd(provider.apply(item)))
                .setHeader(header)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END)
                .setSortable(true)
                .setResizable(true);

        column.setPartNameGenerator(item -> {
            BigDecimal value = provider.apply(item);
            return value != null && value.signum() < 0
                    ? "payroll-money payroll-money-negative"
                    : "payroll-money";
        });
    }

    static Span statusBadge(String status) {
        Span badge = new Span(status == null ? "-" : statusLabel(status));
        badge.addClassName("payroll-status-badge");
        String theme = switch (nvl(status)) {
            case "PAID", "APPROVED", "CLOSED", "INCLUDED", "ACTIVE", "SETTLED" -> "badge success pill";
            case "CANCELLED", "FAILED", "STALE", "EXCLUDED", "NOT_CONFIGURED" -> "badge error pill";
            case "PENDING_REVIEW", "REVIEWED", "CALCULATED", "PROCESSING", "ON_HOLD", "PAUSED" -> "badge primary pill";
            default -> "badge contrast pill";
        };
        badge.getElement().getThemeList().add(theme);
        return badge;

    }

    static Span typeBadge(String type) {
        Span badge = new Span(componentTypeLabel(type));
        badge.addClassName("payroll-type-badge");
        badge.getElement().getThemeList().add(switch (nvl(type)) {
            case "EARNING" -> "badge success";
            case "DEDUCTION" -> "badge error";
            default -> "badge primary";
        });
        return badge;
    }

    static boolean isSystemCalculatedItem(ItemRow item) {

        if (item == null) {
            return false;
        }

        if (Set.of(

                "BASIC_SALARY", "SALARY_TAX",

                "NSSF_EMPLOYEE", "NSSF_EMPLOYER",

                "NSSF_HEALTH_EMPLOYEE", "NSSF_HEALTH_EMPLOYER",

                "NSSF_RISK_EMPLOYEE", "NSSF_RISK_EMPLOYER",

                "NSSF_PENSION_EMPLOYEE", "NSSF_PENSION_EMPLOYER",

                "ABSENCE_DEDUCTION", "SICK_LEAVE_DEDUCTION", "MATERNITY_DEDUCTION",

                "OVERTIME_PAY", "PAYROLL_ADJUSTMENT_EARNING",

                "PAYROLL_ADJUSTMENT_DEDUCTION",
                "FIXED_INCOME", "COMPANY_LOAN_REPAYMENT").contains(item.componentCode())) {

            return true;

        }

        if (Set.of("RECURRING", "LOAN").contains(nvl(item.sourceType()))) {
            return true;
        }

        return "ALLOWANCE".equals(item.componentCode())  && "ATTENDANCE".equals(item.sourceType())  && nvl(item.remarks()).matches( "^Rule (ADP|ATC|STC|ASO|CHS|CLT|HST|STR|SUC);.*");

    }

    static List<PayrollCandidateRow> filterPayrollCandidates(

            List<PayrollCandidateRow> candidates, String searchText) {

        String term = nvl(searchText).trim().toLowerCase(java.util.Locale.ROOT);

        if (term.isBlank()) {
            return candidates;
        }

        return candidates.stream()

                .filter(candidate -> String.join(" ",

                        candidate.insuranceNo() == null ? "" : candidate.insuranceNo().toString(),

                        nvl(candidate.nameEn()),

                        nvl(candidate.nameKh()),

                        candidate.lastCareerTypeDate() == null

                                ? "" : candidate.lastCareerTypeDate().toString(),

                        candidate.referenceNetPay() == null

                                ? "" : candidate.referenceNetPay().stripTrailingZeros().toPlainString())

                        .toLowerCase(java.util.Locale.ROOT)

                        .contains(term))

                .toList();

    }

    static String usd(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal rounded = safe.setScale(2, RoundingMode.HALF_UP);

        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        String absoluteAmount = format.format(rounded.abs());

        return rounded.signum() < 0 ? "-$" + absoluteAmount : "$" + absoluteAmount;
    }

    static String currencyMoney(BigDecimal amount, String currency) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        BigDecimal rounded = safe.setScale(2, RoundingMode.HALF_UP);
        String normalized = nvl(currency).toUpperCase(Locale.ROOT);
        String prefix = "USD".equals(normalized) ? "$" : normalized + " ";

        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        String absoluteAmount = format.format(rounded.abs());
        String sign = rounded.signum() < 0 ? "-" : "";

        return sign + prefix + absoluteAmount;
    }

    static String wholeNumber(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    static String decimal(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

  /*  static String formatDateTime(java.time.OffsetDateTime value) {

        return value.atZoneSameInstant(ZoneId.of("Asia/Phnom_Penh"))

                .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));

    }
    */

    static String statusLabel(String status) {

        return switch (nvl(status)) {

            case "OPEN" -> "OPEN";

            case "DRAFT" -> "DRAFT";

            case "CHECKED" -> "CHECKED";

            case "CALCULATED" -> "CALCULATED";

            case "PENDING_REVIEW" -> "PENDING REVIEW";

            case "REVIEWED" -> "REVIEWED";

            case "APPROVED" -> "APPROVED";

            case "PAID" -> "PAID";

            case "PROCESSING" -> "PROCESSING";

            case "CLOSED" -> "CLOSED";

            case "CANCELLED" -> "CANCELLED";

            case "FAILED" -> "FAILED";

            case "STALE" -> "STALE";

            case "INCLUDED" -> "INCLUDED";

            case "ON_HOLD" -> "ON HOLD";

            case "EXCLUDED" -> "EXCLUDED";

            case "NOT_CONFIGURED" -> "NOT CONFIGURED";

            default -> nvl(status).replace('_', ' ');

        };

    }

    static String runTypeLabel(String type) {

        return switch (nvl(type)) {

            case "REGULAR" -> "REGULAR";

            case "ADJUSTMENT" -> "ADJUSTMENT";

            case "FINAL_PAYMENT" -> "FINAL PAYMENT";

            default -> nvl(type).replace('_', ' ');

        };

    }

    static String runTypeDescription(String type) {

        return switch (nvl(type)) {

            case "REGULAR" -> "Generate and calculate every INCLUDED rostered employee for the monthly payroll. "

                    + "| បង្កើត និងគណនាបុគ្គលិក INCLUDED ទាំងអស់ក្នុងតារាងវេនសម្រាប់ប្រាក់បៀវត្សប្រចាំខែ។";

            case "ADJUSTMENT" -> "Select affected employees and calculate only the difference from the approved regular run. "

                    + "| ជ្រើសបុគ្គលិកដែលត្រូវកែតម្រូវ ហើយគណនាតែចំនួនខុសគ្នាពីដំណើរការប្រចាំខែដែលបានអនុម័ត។";

            case "FINAL_PAYMENT" -> "Select departing employees and calculate salary and attendance through their contract end date. "

                    + "| ជ្រើសបុគ្គលិកដែលចាកចេញ ហើយគណនាប្រាក់បៀវត្ស និងវត្តមានត្រឹមថ្ងៃបញ្ចប់កិច្ចសន្យា។";

            default -> "";

        };

    }

    static String installmentTypeLabel(String type) {

        return switch (nvl(type)) {

            case "FIRST_INSTALLMENT" -> "FIRST INSTALLMENT";

            case "FINAL_SETTLEMENT" -> "FINAL SETTLEMENT";

            case "ADJUSTMENT_SETTLEMENT" -> "ADJUSTMENT SETTLEMENT";

            default -> nvl(type).replace('_', ' ');

        };

    }

    static String frequencyLabel(String frequency) {

        return switch (nvl(frequency)) {

            case "SEMI_MONTHLY" -> "SEMI-MONTHLY";

            case "MONTHLY" -> "MONTHLY";

            default -> nvl(frequency).replace('_', ' ');

        };

    }

    static String periodLabel(PeriodRow period) {

        String range = period.startDate() == null || period.endDate() == null

                ? ""

                : " · %s → %s".formatted(period.startDate(), period.endDate());

        return "%04d-%02d%s · %s".formatted(

                period.year(), period.month(), range, statusLabel(period.status()));

    }

    static String runLabel(RunRow run) {

        return "Run  %d · %s · %s".formatted(

                run.runNumber(), runTypeLabel(run.runType()), statusLabel(run.status()));

    }

    static String componentTypeLabel(String type) {

        return switch (nvl(type)) {

            case "EARNING" -> "EARNING | ចំណូល";

            case "DEDUCTION" -> "DEDUCTION | ការកាត់";

            case "EMPLOYER_CONTRIBUTION" -> "EMPLOYER CONTRIBUTION | ភាគទាននិយោជក";

            default -> nvl(type).replace('_', ' ');

        };

    }

    static String sourceTypeLabel(String source) {

        return switch (nvl(source)) {

            case "MANUAL" -> "MANUAL | បញ្ចូលដោយដៃ";

            case "SALARY" -> "SALARY | ប្រាក់បៀវត្ស";

            case "ATTENDANCE" -> "ATTENDANCE | វត្តមាន";

            case "LEAVE" -> "LEAVE | ច្បាប់";

            case "OVERTIME" -> "OVERTIME | ម៉ោងបន្ថែម";

            case "TAX" -> "TAX | ពន្ធ";

            case "NSSF" -> "NSSF | ប.ស.ស.";

            case "SENIORITY" -> "SENIORITY | ប្រាក់បំណាច់អតីតភាពការងារ";

            case "RECURRING" -> "RECURRING | ចំណូល/ការកាត់ប្រចាំ";

            case "LOAN" -> "LOAN / RECOVERY | ប្រាក់កម្ចី / ការសងសំណង";

            case "ROSTER" -> "ROSTER | តារាងវេន";

            case "SYSTEM" -> "SYSTEM | ប្រព័ន្ធ";

            default -> nvl(source).replace('_', ' ');

        };

    }

    static String khmerMonth(int month) {

        return switch (month) {

            case 1 -> "មករា";

            case 2 -> "កុម្ភៈ";

            case 3 -> "មីនា";

            case 4 -> "មេសា";

            case 5 -> "ឧសភា";

            case 6 -> "មិថុនា";

            case 7 -> "កក្កដា";

            case 8 -> "សីហា";

            case 9 -> "កញ្ញា";

            case 10 -> "តុលា";

            case 11 -> "វិច្ឆិកា";

            case 12 -> "ធ្នូ";

            default -> "";

        };

    }

    static String khmerWeekdayShort(LocalDate date) {

        return switch (date.getDayOfWeek()) {

            case MONDAY -> "ច";

            case TUESDAY -> "អ";

            case WEDNESDAY -> "ព";

            case THURSDAY -> "ព្រ";

            case FRIDAY -> "សុ";

            case SATURDAY -> "ស";

            case SUNDAY -> "អា";

        };

    }

    static String nvl(String value) {

        return value == null ? "" : value;

    }

    static void success(String message) {

        Notification notification = Notification.show(message, 7000, Notification.Position.TOP_CENTER);

        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

    }

    static void error(String message) {
        Notification notification = Notification.show(message, 10000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    static String message(Exception ex) {

        Throwable cause = ex;

        while (cause.getCause() != null && cause.getCause() != cause) {

            cause = cause.getCause();

        }

        return cause.getMessage() == null ? ex.getClass().getSimpleName() : cause.getMessage();

    }

    static String databaseMessage(Exception ex) {

        String detail = message(ex);

        if (detail.contains("payroll_attendance_control")) {

            return "Attendance-control table is not available. "

                    + "Run V8__create_payroll_attendance_control.sql first. "

                    + "| មិនមានតារាងគ្រប់គ្រងវត្តមានទេ។ សូមដំណើរការ "

                    + "V8__create_payroll_attendance_control.sql ជាមុនសិន។";

        }

        if (detail.contains("payroll_period") || detail.contains("payroll_component")) {

            return "Payroll tables are not available. Run V7__create_payroll_module.sql first. "

                    + "| មិនមានតារាងប្រាក់បៀវត្សទេ។ សូមដំណើរការ "

                    + "V7__create_payroll_module.sql ជាមុនសិន។";

        }

        return detail;

    }
    static String attendanceControlMessage(AttendanceControlRow control) {

        if (control.stale()) {

            return "Roster, attendance, or leave-balance data changed after the last check or approval. "

                    + "Check and approve attendance again before payroll. "

                    + "| ទិន្នន័យតារាងវេន វត្តមាន ឬសមតុល្យច្បាប់បានផ្លាស់ប្ដូរបន្ទាប់ពីការពិនិត្យ "

                    + "ឬអនុម័តចុងក្រោយ។ សូមពិនិត្យ និងអនុម័តវត្តមានម្ដងទៀតមុនគណនាប្រាក់បៀវត្ស។";

        }

        if (PayrollAttendanceControlStatus.APPROVED.matches(control.status())) {

            String approval = control.approvedBy() == null ? ""

                    : " Approved by | អនុម័តដោយ " + control.approvedBy()

                    + (control.approvedAt() == null

                            ? "."

                            : " on | នៅថ្ងៃ " + DateTimeUtilFormart.DATE_TIME_FORMATTER.format(control.approvedAt())  + ".");

            return "Attendance is approved and locked for payroll. "

                    + "| វត្តមានត្រូវបានអនុម័ត និងចាក់សោសម្រាប់ប្រាក់បៀវត្ស។" + approval;

        }

        if (PayrollAttendanceControlStatus.CHECKED.matches(control.status())) {

            if (control.rosterEmployeeCount() == 0 || control.rosterDayCount() == 0) {

                return "No roster data was found. Complete the roster before attendance approval. "

                        + "| មិនឃើញទិន្នន័យតារាងវេនទេ។ សូមបំពេញតារាងវេនមុនអនុម័តវត្តមាន។";

            }

            if (control.unverifiedAttendanceCount() > 0) {

                return control.unverifiedAttendanceCount()

                        + " attendance record(s) still need HR final verification. "

                        + "Complete verification, then click Approve & Lock again. "

                        + "| កំណត់ត្រាវត្តមានចំនួន " + control.unverifiedAttendanceCount()

                        + " នៅត្រូវការ HR ផ្ទៀងផ្ទាត់ចុងក្រោយ។ សូមបញ្ចប់ការផ្ទៀងផ្ទាត់ "

                        + "បន្ទាប់មកចុច អនុម័ត និងចាក់សោ ម្ដងទៀត។";

            }

            return "Attendance is ready. Review the totals, then click Approve & Lock. "

                    + "| វត្តមានរួចរាល់។ សូមពិនិត្យចំនួនសរុប បន្ទាប់មកចុច អនុម័ត និងចាក់សោ។";

        }

        return "Complete the roster and HR final attendance review, then click Approve & Lock. "

                + "| សូមបំពេញតារាងវេន និងការពិនិត្យវត្តមានចុងក្រោយរបស់ HR "

                + "បន្ទាប់មកចុច អនុម័ត និងចាក់សោ។";

    }

    static Div attendanceMetric(String label, String value) {
        Div metric = new Div();
        metric.addClassName("payroll-attendance-metric");
        Span metricLabel = new Span(label);
        metricLabel.addClassName("payroll-attendance-metric-label");
        Span metricValue = new Span(value);
        metricValue.addClassName("payroll-attendance-metric-value");
        metric.add(metricLabel, metricValue);
        return metric;
    }


}

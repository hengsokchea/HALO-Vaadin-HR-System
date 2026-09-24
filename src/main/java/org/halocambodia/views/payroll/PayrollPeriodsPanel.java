package org.halocambodia.views.payroll;

import static org.halocambodia.data.PayrollModels.*;
import static org.halocambodia.views.payroll.PayrollViewSupport.*;

import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.security.PayrollActionPermissions;
import org.halocambodia.services.PayrollService;

import org.halocambodia.views.CustomDialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;

final class PayrollPeriodsPanel extends VerticalLayout {

    private final PayrollService service;
    private final AuthenticatedUser authenticatedUser;
    private final PayrollRefreshCoordinator refreshCoordinator;
    private final Grid<PeriodRow> periodGrid = new Grid<>();

    PayrollPeriodsPanel(PayrollService service, AuthenticatedUser authenticatedUser, PayrollRefreshCoordinator refreshCoordinator) {
        this.service = service;
        this.authenticatedUser = authenticatedUser;
        this.refreshCoordinator = refreshCoordinator;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        configurePeriodGrid();
        VerticalLayout content = buildPage();
        add(content);
        setFlexGrow(1, content);
    }

    void refresh() {
        try {
            periodGrid.setItems(service.findPeriods());
        } catch (Exception ex) {
            error(databaseMessage(ex));
        }
    }

    private VerticalLayout buildPage() {
        VerticalLayout page = page("Payroll Periods | រយៈពេលបើកប្រាក់ខែ", "Create one controlled period for each payroll month. | បង្កើតរយៈពេលដែលបានគ្រប់គ្រងមួយ សម្រាប់ខែបើកប្រាក់ខែនីមួយៗ។ " );

        Button add = primaryButton("New Period", VaadinIcon.PLUS_CIRCLE);
        Button edit = actionButton("Edit", VaadinIcon.EDIT);
        Button delete = actionButton("Delete", VaadinIcon.TRASH);
        Button refresh = actionButton("Refresh", VaadinIcon.REFRESH);

        add.setEnabled(authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.INSERTED_PAGE));
        edit.setEnabled(authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.UPDATED_PAGE));
        delete.setEnabled(authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.DELETED_PAGE));

        add.addClickListener(e -> openPeriodDialog(null));
        edit.addClickListener(e -> selectedPeriod().ifPresentOrElse(this::openPeriodDialog,() -> error("Select one payroll period to edit. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សមួយដើម្បីកែប្រែ។")));
        delete.addClickListener(e -> selectedPeriod().ifPresentOrElse(this::confirmDeletePeriod,() -> error("Select one payroll period to delete. | សូមជ្រើសរើសរយៈពេលប្រាក់បៀវត្សមួយដើម្បីលុប។")));
        refresh.addClickListener(e -> refresh());

        HorizontalLayout toolbar = toolbar(add, edit, delete, refresh);
        page.add(toolbar, periodGrid);
        page.setFlexGrow(1, periodGrid);
        return page;
    }

    private void configurePeriodGrid() {
        configureGrid(periodGrid);
        periodGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        periodGrid.addColumn(PeriodRow::year).setHeader("Year").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::month).setHeader("Month").setAutoWidth(true).setSortable(true).setResizable(true);
        
     
        
        periodGrid.addColumn(PeriodRow::startDate).setHeader("Start Date").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::endDate).setHeader("End Date").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::paymentDate).setHeader("Payment Date").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::currency).setHeader("Currency").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::usdToKhrRate).setHeader("Payroll/Tax USD to KHR").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::nssfUsdToKhrRate).setHeader("NSSF USD to KHR").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addComponentColumn(p -> statusBadge(p.status())).setHeader("Status").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::runCount).setHeader("Runs").setAutoWidth(true).setSortable(true).setResizable(true);
        periodGrid.addColumn(PeriodRow::employeeCount).setHeader("Employees").setAutoWidth(true).setSortable(true).setResizable(true);
        moneyColumn(periodGrid, "Net Pay", PeriodRow::netPay);
        periodGrid.addItemDoubleClickListener(e -> {
            if (authenticatedUser.hasPermissionRoute(PayrollActionPermissions.PERIOD_MANAGEMENT, AccessPageType.UPDATED_PAGE)) {
                openPeriodDialog(e.getItem());
            }
        });
    }

    private Optional<PeriodRow> selectedPeriod() {
        return periodGrid.getSelectedItems().stream().findFirst();
    }

    private void openPeriodDialog(PeriodRow current) {
        CustomDialog dialog = editorDialog(current == null
                ? "New Payroll Period | បង្កើតរយៈពេលថ្មី"
                : "Edit Payroll Period | កែប្រែរយៈពេល");

        IntegerField year = new IntegerField("Payroll Year");
        Select<Integer> month = new Select<>();
        month.setLabel("Payroll Month");
        month.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        month.setItemLabelGenerator(value -> "%02d - %s | %s".formatted( value, java.time.Month.of(value).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH), khmerMonth(value)));

        com.vaadin.flow.component.datepicker.DatePicker start = new com.vaadin.flow.component.datepicker.DatePicker("Start Date");
        com.vaadin.flow.component.datepicker.DatePicker end =  new com.vaadin.flow.component.datepicker.DatePicker("End Date");
        com.vaadin.flow.component.datepicker.DatePicker payment = new com.vaadin.flow.component.datepicker.DatePicker("Payment Date");

        ComboBox<String> currency = new ComboBox<>("Currency");
        currency.setItems("USD", "KHR");
        currency.setItemLabelGenerator(value -> switch (value) {
            case "USD" -> "USD - US Dollar";
            case "KHR" -> "KHR - Cambodian Riel";
            default -> value;
        });
        currency.setRequiredIndicatorVisible(true);
        currency.setClearButtonVisible(false);

        BigDecimalField exchangeRate = new BigDecimalField("Payroll/Tax USD to KHR Rate");
        exchangeRate.setPlaceholder("Finance/Tax monthly rate");
        exchangeRate.setRequiredIndicatorVisible(true);

        BigDecimalField nssfExchangeRate = new BigDecimalField("NSSF USD to KHR Rate");
        nssfExchangeRate.setPlaceholder("Enter the NSSF monthly exchange rate");
        nssfExchangeRate.setRequiredIndicatorVisible(true);

        Select<String> status = new Select<>();
        status.setLabel("Status");
        status.setItems(PERIOD_STATUSES);
        status.setItemLabelGenerator(PayrollViewSupport::statusLabel);
        status.setReadOnly(true);

        if (current == null) {
            YearMonth now = YearMonth.now();
            year.setValue(now.getYear());
            month.setValue(now.getMonthValue());
            start.setValue(now.atDay(1));
            end.setValue(now.atEndOfMonth());
            payment.setValue(now.atEndOfMonth());
            currency.setValue("USD");
            status.setValue("OPEN");
        } else {
            year.setValue(current.year());
            month.setValue(current.month());
            start.setValue(current.startDate());
            end.setValue(current.endDate());
            payment.setValue(current.paymentDate());
            currency.setValue(Set.of("USD", "KHR").contains(current.currency())  ? current.currency() : "USD");
            exchangeRate.setValue(current.usdToKhrRate());
            nssfExchangeRate.setValue(current.nssfUsdToKhrRate());
            status.setValue(current.status());
        }

        month.addValueChangeListener(e -> {
            if (year.getValue() != null && e.getValue() != null) {
                YearMonth ym = YearMonth.of(year.getValue(), e.getValue());
                start.setValue(ym.atDay(1));
                end.setValue(ym.atEndOfMonth());
            }
        });

        FormLayout form = form(
                year, month, start, end, payment, currency,
                exchangeRate, nssfExchangeRate, status);
        dialog.add(form);

        Button save = primaryButton("Save", VaadinIcon.CHECK);
        Button cancel = new Button("Cancel", e -> dialog.close());

        save.addClickListener(e -> {
            try {
                if (year.getValue() == null || month.getValue() == null || currency.getValue() == null) {
                    throw new IllegalArgumentException("Payroll year, month, and currency are required. " );
                }

                Long savedPeriodId = service.savePeriod(new PeriodInput(
                        current == null ? null : current.id(), year.getValue(), month.getValue(),
                        start.getValue(), end.getValue(), payment.getValue(),
                        currency.getValue(), exchangeRate.getValue(),
                        nssfExchangeRate.getValue(), status.getValue()));

                dialog.close();
                refresh();
                refreshCoordinator.refreshProcessing();
                refreshCoordinator.refreshPaymentPeriod(savedPeriodId);

                PaymentScheduleSummary paymentSchedule = refreshCoordinator.paymentSchedule();
                String scheduleMessage = paymentSchedule == null
                        ? ""
                        : " Detected schedule: "
                                + (paymentSchedule.mixedSchedule()
                                        ? "MIXED | ចម្រុះ"
                                        : paymentSchedule.hasSemiMonthlyEmployees()
                                                ? frequencyLabel("SEMI_MONTHLY")
                                                : frequencyLabel("MONTHLY"))
                                + "; Monthly " + paymentSchedule.monthlyEmployeeCount()
                                + "; Semi-Monthly " + paymentSchedule.semiMonthlyEmployeeCount()
                                + "; Shift Exception Staff " + paymentSchedule.shiftOverrideCount()
                                + ".";

                success("Payroll period saved successfully." + scheduleMessage  + " | បានរក្សាទុករយៈពេល និងរកកាលវិភាគបើកប្រាក់រួច។");
            } catch (Exception ex) {
                error(message(ex));
            }
        });

        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void confirmDeletePeriod(PeriodRow period) {
        PayrollConfirmDialog confirm = confirm(
                "Delete Payroll Period | លុបរយៈពេលប្រាក់បៀវត្ស",
                ("Delete %04d-%02d? This is allowed only when the period has no payroll runs. "
                        + "| លុប %04d-%02d មែនទេ? អាចលុបបានតែនៅពេលរយៈពេលនេះមិនមានដំណើរការប្រាក់បៀវត្ស។")
                        .formatted(period.year(), period.month(), period.year(), period.month()));
        confirm.addConfirmListener(e -> {
            try {
                service.deletePeriod(period.id());
                refresh();
                refreshCoordinator.refreshProcessing();
                success("Payroll period deleted. | បានលុបរយៈពេលប្រាក់បៀវត្ស។");
            } catch (Exception ex) {
                error(message(ex));
            }
        });
        confirm.open();
    }
}

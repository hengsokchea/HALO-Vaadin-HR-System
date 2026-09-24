package org.halocambodia.views.payroll;

import java.util.function.LongConsumer;
import java.util.function.Supplier;

import org.halocambodia.data.PayrollModels.PaymentScheduleSummary;

final class PayrollRefreshCoordinator {

    private Runnable dashboardRefresh = () -> { };
    private Runnable periodsRefresh = () -> { };
    private Runnable attendanceRefresh = () -> { };
    private Runnable processingRefresh = () -> { };
    private Runnable paymentsRefresh = () -> { };
    private LongConsumer paymentPeriodRefresh = ignored -> { };
    private Supplier<PaymentScheduleSummary> paymentScheduleSupplier = () -> null;
    private Supplier<Long> processingPeriodIdSupplier = () -> null;

    void registerDashboard(Runnable refresh) {
        dashboardRefresh = safe(refresh);
    }

    void registerPeriods(Runnable refresh) {
        periodsRefresh = safe(refresh);
    }

    void registerAttendance(Runnable refresh) {
        attendanceRefresh = safe(refresh);
    }

    void registerProcessing(Runnable refresh) {
        processingRefresh = safe(refresh);
    }

    void registerPayments(Runnable refresh) {
        paymentsRefresh = safe(refresh);
    }

    void registerPaymentPeriodRefresh(LongConsumer refresh) {
        paymentPeriodRefresh = refresh == null ? ignored -> { } : refresh;
    }

    void registerPaymentScheduleSupplier(Supplier<PaymentScheduleSummary> supplier) {
        paymentScheduleSupplier = supplier == null ? () -> null : supplier;
    }

    void registerProcessingPeriodIdSupplier(Supplier<Long> supplier) {
        processingPeriodIdSupplier = supplier == null ? () -> null : supplier;
    }

    void refreshDashboard() {
        dashboardRefresh.run();
    }

    void refreshPeriods() {
        periodsRefresh.run();
    }

    void refreshAttendance() {
        attendanceRefresh.run();
    }

    void refreshProcessing() {
        processingRefresh.run();
    }

    void refreshPayments() {
        paymentsRefresh.run();
    }

    void refreshPaymentPeriod(Long periodId) {
        paymentPeriodRefresh.accept(periodId);
    }

    PaymentScheduleSummary paymentSchedule() {
        return paymentScheduleSupplier.get();
    }

    Long processingPeriodId() {
        return processingPeriodIdSupplier.get();
    }

    private static Runnable safe(Runnable runnable) {
        return runnable == null ? () -> { } : runnable;
    }
}

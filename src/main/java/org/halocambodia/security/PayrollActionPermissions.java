package org.halocambodia.security;

/** Permission route keys used by Payroll action-level authorization. */
public final class PayrollActionPermissions {
    public static final String PERIOD_MANAGEMENT = "payroll-action-period-management";
    public static final String ATTENDANCE_LOCK = "payroll-action-attendance-lock";
    public static final String RUN_MANAGEMENT = "payroll-action-run-management";
    public static final String EMPLOYEE_STATUS = "payroll-action-employee-status";
    public static final String MANUAL_ITEM = "payroll-action-manual-item";
    public static final String CALCULATE = "payroll-action-calculate";
    public static final String SEND_REVIEW = "payroll-action-send-review";
    public static final String REVIEW = "payroll-action-review";
    public static final String RETURN_CORRECTION = "payroll-action-return-correction";
    public static final String APPROVE = "payroll-action-approve";
    public static final String PAYMENT_GENERATE = "payroll-action-payment-generate";
    public static final String PAYMENT_APPROVE = "payroll-action-payment-approve";
    public static final String BANK_EXPORT = "payroll-action-bank-export";
    public static final String PAYMENT_RECONCILE = "payroll-action-payment-reconcile";
    public static final String PAYSLIP_EMAIL_SEND = "payroll-action-payslip-email-send";

    private PayrollActionPermissions() {}
}

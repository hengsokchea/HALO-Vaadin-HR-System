package org.halocambodia.enums;

import java.util.List;

public enum PayrollRunStatus implements PayrollEnumValue {
    DRAFT,
    CALCULATED,
    PENDING_REVIEW,
    REVIEWED,
    APPROVED,
    PAID,
    CANCELLED;

    public static PayrollRunStatus from(String value) {
        return PayrollEnumValue.parse(PayrollRunStatus.class, value, "payroll run status");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

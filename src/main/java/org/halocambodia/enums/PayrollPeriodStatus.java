package org.halocambodia.enums;

import java.util.List;

public enum PayrollPeriodStatus implements PayrollEnumValue {
    OPEN,
    PROCESSING,
    CLOSED;

    public static PayrollPeriodStatus from(String value) {
        return PayrollEnumValue.parse(PayrollPeriodStatus.class, value, "payroll period status");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

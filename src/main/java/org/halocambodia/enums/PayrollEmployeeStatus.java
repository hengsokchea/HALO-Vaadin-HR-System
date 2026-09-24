package org.halocambodia.enums;

import java.util.List;

public enum PayrollEmployeeStatus implements PayrollEnumValue {
    INCLUDED,
    ON_HOLD,
    EXCLUDED;

    public static PayrollEmployeeStatus from(String value) {
        return PayrollEnumValue.parse(PayrollEmployeeStatus.class, value, "payroll employee status");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

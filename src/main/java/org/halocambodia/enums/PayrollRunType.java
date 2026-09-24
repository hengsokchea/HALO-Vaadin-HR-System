package org.halocambodia.enums;

import java.util.List;

public enum PayrollRunType implements PayrollEnumValue {
    REGULAR,
    ADJUSTMENT,
    FINAL_PAYMENT;

    public static PayrollRunType from(String value) {
        return PayrollEnumValue.parse(PayrollRunType.class, value, "payroll run type");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

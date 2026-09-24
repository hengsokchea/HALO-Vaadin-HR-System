package org.halocambodia.enums;

import java.util.List;

public enum PayrollInstallmentType implements PayrollEnumValue {
    FIRST_INSTALLMENT,
    FINAL_SETTLEMENT,
    ADJUSTMENT_SETTLEMENT;

    public static PayrollInstallmentType from(String value) {
        return PayrollEnumValue.parse(PayrollInstallmentType.class, value, "payment installment type");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

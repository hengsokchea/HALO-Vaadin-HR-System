package org.halocambodia.enums;

import java.util.List;

public enum PayrollPaymentBatchStatus implements PayrollEnumValue {
    DRAFT,
    APPROVED,
    PAID,
    CANCELLED;

    public static PayrollPaymentBatchStatus from(String value) {
        return PayrollEnumValue.parse(PayrollPaymentBatchStatus.class, value, "payment batch status");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

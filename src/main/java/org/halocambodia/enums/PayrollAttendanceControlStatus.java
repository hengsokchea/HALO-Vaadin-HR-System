package org.halocambodia.enums;

import java.util.List;

public enum PayrollAttendanceControlStatus implements PayrollEnumValue {
    DRAFT,
    CHECKED,
    APPROVED;

    public static PayrollAttendanceControlStatus from(String value) {
        return PayrollEnumValue.parse(PayrollAttendanceControlStatus.class, value, "attendance control status");
    }

    public static List<String> codes() {
        return PayrollEnumValue.codes(values());
    }
}

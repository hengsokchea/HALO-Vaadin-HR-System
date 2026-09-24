package org.halocambodia.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Common database-code behaviour for payroll enums stored as uppercase text. */
public interface PayrollEnumValue {

    default String code() {
        return ((Enum<?>) this).name();
    }

    default boolean matches(String value) {
        return value != null && code().equals(value.trim().toUpperCase(Locale.ROOT));
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported " + label + ": " + value + ".", ex);
        }
    }

    static List<String> codes(PayrollEnumValue[] values) {
        return Arrays.stream(values).map(PayrollEnumValue::code).toList();
    }
}

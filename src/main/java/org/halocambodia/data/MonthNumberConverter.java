package org.halocambodia.data;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class MonthNumberConverter implements Converter<String, Integer> {
    @Override
    public Result<Integer> convertToModel(String presentationValue, ValueContext context) {
        if (presentationValue == null || presentationValue.trim().isEmpty()) {
            return Result.ok(null); // Handle null or empty input
        }
        try {
            Month month = Month.valueOf(presentationValue.toUpperCase());
            return Result.ok(month.getValue());
        } catch (IllegalArgumentException e) {
            return Result.error("Invalid month value: " + presentationValue);
        }
    }

    @Override
    public String convertToPresentation(Integer modelValue, ValueContext context) {
        if (modelValue == null) {
            return "";
        }
        return Month.of(modelValue).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
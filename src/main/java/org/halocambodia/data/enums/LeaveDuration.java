package org.halocambodia.data.enums;

public enum LeaveDuration {
    MORNING_ONLY("Morning only"),
    AFTERNOON_ONLY("Afternoon only"),
    FULL_DAY("Full day");

    private final String label;

    LeaveDuration(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package org.halocambodia.enums;
public enum AttendanceEntryStatus {
    DRAFT("Draft | ព្រាង"),
    SUBMITTED("Submitted | បានបញ្ជូន");

    private final String label;

    AttendanceEntryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
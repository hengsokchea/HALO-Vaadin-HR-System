package org.halocambodia.enums;

public enum AttendanceQcStatus {
    PENDING("Pending QC | រង់ចាំ QC"),
    VERIFIED("QC Verified | QC បានផ្ទៀងផ្ទាត់"),
    RETURNED("Returned | បានបញ្ជូនត្រឡប់");
    private final String label;
    AttendanceQcStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}

package org.halocambodia.enums;

public enum EducationStatusEnum {
    ONGOING("Ongoing"),
    INCOMPLETE("Incomplete"),
    COMPLETED("Completed");

    private final String label;

    EducationStatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

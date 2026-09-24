package org.halocambodia.enums;

public enum GenderEnum {
	male("Male"),
    female("Female");

    private final String label;

    GenderEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

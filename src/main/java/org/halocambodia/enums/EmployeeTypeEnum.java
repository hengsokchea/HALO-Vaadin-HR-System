package org.halocambodia.enums;

public enum EmployeeTypeEnum {
	National("National Staff"),
    International("International Staff");

    private final String label;

    EmployeeTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}

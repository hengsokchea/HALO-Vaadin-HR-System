package org.halocambodia.data;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public  class RosterRow {
    private final Employee employee;
    private final Map<LocalDate, EmployeeRoster> rosterByDate = new LinkedHashMap<>();
    private EmployeeAllocation employeeAllocation;

    public RosterRow(Employee employee) {
        this.employee = employee;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmployeeRoster getRosterFor(LocalDate date) {
        return rosterByDate.get(date);
    }

    public void putRoster(LocalDate date, EmployeeRoster roster) {
        rosterByDate.put(date, roster);
        if (this.employeeAllocation == null && roster != null) {
            this.employeeAllocation = roster.getEmployeeAllocation();
        }
    }
    public EmployeeAllocation getEmployeeAllocation() {
        if (employeeAllocation != null) {
            return employeeAllocation;
        }

        // Fallback: look through the map in case it wasn't initialized
        return rosterByDate.values().stream()
                .map(EmployeeRoster::getEmployeeAllocation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
    public ShiftCycle findFallbackShiftCycle() {
        return rosterByDate.values().stream()
                .map(EmployeeRoster::getShiftCycle)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}

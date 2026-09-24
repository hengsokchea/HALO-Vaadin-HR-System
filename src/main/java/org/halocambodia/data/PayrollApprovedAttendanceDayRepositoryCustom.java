package org.halocambodia.data;

public interface PayrollApprovedAttendanceDayRepositoryCustom {

    int captureApprovedAttendance(Long payrollPeriodId, Long approvedBy);
}

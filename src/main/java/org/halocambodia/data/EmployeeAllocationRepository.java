package org.halocambodia.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.halocambodia.data.*;

public interface EmployeeAllocationRepository  extends JpaRepository<EmployeeAllocation, Long>, JpaSpecificationExecutor<EmployeeAllocation> {


	boolean existsByEmployeeAndYearNumAndMonthNum(Employee employee, Integer yearNum, Integer monthNum);

	Optional<EmployeeAllocation> findByEmployeeAndYearNumAndMonthNum(Employee employee, Integer yearNum, Integer monthNum);
	
	List<EmployeeAllocation> findByMonthNumAndYearNum(int monthNum, int yearNum);

	default Optional<Shift> findShiftByEmployeeAndDate(Employee employee, LocalDate date) {
	    if (employee == null || date == null) {
	        return Optional.empty();
	    }

	    return findByEmployeeAndYearNumAndMonthNum(employee, date.getYear(), date.getMonthValue())
	            .map(a -> a.getShiftCycle() != null ? a.getShiftCycle().getShift() : null)
	            .filter(Objects::nonNull);
	}



}

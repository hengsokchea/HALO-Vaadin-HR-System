package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HREmployeeRepository  extends JpaRepository<HREmployeeData, Long>, JpaSpecificationExecutor<HREmployeeData> {
	
	List<HREmployeeData> findByStatusAndEmployeeType(String status, String employeeType);

	default List<HREmployeeData> findActiveLocalStaff() {
	    return findByStatusAndEmployeeType("Active", "Local Staff");
	}

	
}

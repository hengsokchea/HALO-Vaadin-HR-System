package org.halocambodia.data;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HREmployeeWithSupervisorRepository  extends JpaRepository<HREmployeeWithSupervisor, Long>, JpaSpecificationExecutor<HREmployeeWithSupervisor> {
	
}

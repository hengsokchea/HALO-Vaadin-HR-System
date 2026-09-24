package org.halocambodia.data;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShiftRepository  extends JpaRepository<Shift, Long>, JpaSpecificationExecutor<Shift> {
	Optional<Shift> findByShiftNameIgnoreCase(String shiftName);
	
}

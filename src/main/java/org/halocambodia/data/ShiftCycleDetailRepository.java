package org.halocambodia.data;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShiftCycleDetailRepository  extends JpaRepository<ShiftCycleDetail, Long>, JpaSpecificationExecutor<ShiftCycleDetail> {
	Optional<ShiftCycleDetail> findByShiftCycleAndCycleDate(ShiftCycle shiftCycle, LocalDate cycleDate);
}

package org.halocambodia.data;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HolidayGroupRepository  extends JpaRepository<HolidayGroup, Long>, JpaSpecificationExecutor<HolidayGroup> {
	 
}

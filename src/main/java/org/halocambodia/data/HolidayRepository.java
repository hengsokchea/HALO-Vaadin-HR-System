package org.halocambodia.data;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HolidayRepository  extends JpaRepository<Holiday, Long>, JpaSpecificationExecutor<Holiday> {
	 List<Holiday> findByHolidayDateBetween(LocalDate startDate, LocalDate endDate);
	 
	 @Query("SELECT h FROM Holiday h WHERE h.holidayDate IS NULL OR (YEAR(h.holidayDate) = :year AND h.id NOT IN :usedIds)")
	 List<Holiday> findByHolidayDateYearAndNotInIds(@Param("year") int year, @Param("usedIds") List<Long> usedIds);
	    
	 @Query("SELECT h FROM Holiday h WHERE h.holidayDate IS NULL OR YEAR(h.holidayDate) = :year ORDER BY h.holidayDate,h.holidayName")
	 List<Holiday> findByHolidayDateYearOrNull(@Param("year") int year);
}

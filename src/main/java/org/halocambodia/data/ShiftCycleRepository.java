package org.halocambodia.data;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ShiftCycleRepository extends JpaRepository<ShiftCycle, Long>, JpaSpecificationExecutor<ShiftCycle> {
	
	Optional<ShiftCycle> findByShift_ShiftNameAndYear(String shiftName, Integer year);
	List<ShiftCycle> findByShift_ShiftNameInAndYear(Collection<String> shiftNames, Integer year);
	
	Optional<ShiftCycle> findByShift_IdAndYear(Integer shiftId, Integer year);
	List<ShiftCycle> findByYear(Integer year);
	Optional<ShiftCycle> findByShift_ShiftNameIgnoreCaseAndYear(String shiftName, Integer year);

	    @Modifying
	    @Transactional
	    @Query(value = """
	        WITH upsert AS (
	            INSERT INTO public.shift_cycle (
	                shift_id, year_number, cycle_start_date, cycle_end_date, created_by, updated_by
	            )
	            VALUES (:shiftId, :yearNumber,
	                    make_date(:yearNumber, 1, 1),
	                    make_date(:yearNumber, 12, 31),
	                    :userId, :userId)
	            ON CONFLICT (shift_id, year_number)
	            DO UPDATE SET
	                cycle_start_date = EXCLUDED.cycle_start_date,
	                cycle_end_date   = EXCLUDED.cycle_end_date,
	                updated_by       = EXCLUDED.updated_by,
	                updated_at       = CURRENT_TIMESTAMP
	            RETURNING shift_cycle_id, cycle_start_date, cycle_end_date
	        )
	        INSERT INTO public.shift_cycle_detail (
	            shift_cycle_id, cycle_date, holiday_id, created_by, updated_by
	        )
	        SELECT
	            u.shift_cycle_id,
	           CAST(d as date),
	            CASE
	              WHEN :includeWeekends = false AND EXTRACT(ISODOW FROM d) IN (6,7)  THEN COALESCE(h.holiday_id,26)
	              WHEN :includeWeekends = false AND EXTRACT(ISODOW FROM d) Not IN (6,7)  THEN COALESCE(h.holiday_id,23)	 	 
	              WHEN :includeWeekends = true   THEN COALESCE(h.holiday_id,23)	             
	              ELSE 23
	            END AS working_day,
	            :userId,
	            :userId
	        FROM upsert u
		        CROSS JOIN generate_series(
		            (SELECT cycle_start_date FROM upsert),
		            (SELECT cycle_end_date   FROM upsert),
		            interval '1 day'
		        ) d
	         LEFT JOIN public.holiday h   ON CAST(h.holiday_date as date) = CAST(d as date)
	        WHERE NOT EXISTS (
	            SELECT 1
	            FROM public.shift_cycle_detail scd
	            WHERE scd.shift_cycle_id = u.shift_cycle_id
	              AND CAST(scd.cycle_date as date) =CAST(d as date)
	        );
	        """, nativeQuery = true)
	    int generateShiftCycleRaw(
	            @Param("shiftId") int shiftId,
	            @Param("yearNumber") int yearNumber,
	            @Param("userId") Long userId,
	            @Param("includeWeekends") boolean includeWeekends
	    );
}


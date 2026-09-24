package org.halocambodia.data;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmployeeRosterRepository  extends JpaRepository<EmployeeRoster, Long>, JpaSpecificationExecutor<EmployeeRoster> {
	
	Optional<EmployeeRoster> findByEmployeeAndRosterDate(Employee employee, LocalDate rosterDate);
	Optional<EmployeeRoster> findByEmployeeIdAndRosterDate(Long employeeId, LocalDate rosterDate);
	
    // ✅ delete rosters linked to allocations
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("delete from EmployeeRoster r where r.employeeAllocation.id in :ids")
    int deleteByEmployeeAllocationIds(@Param("ids") Collection<Long> ids);
    
    // For Grid lazy loading (recommended)
    String ROSTER_GRID_GRAPH = "employee,holiday,holiday.holidayGroup," +
            "employeeAllocation,employeeAllocation.branch,employeeAllocation.teams," +
            "employeeAllocation.positions,employeeAllocation.shiftCycle,employeeAllocation.shiftCycle.shift";

    @Override
    @EntityGraph(attributePaths = {
            "employee",
            "holiday",
            "holiday.holidayGroup",
            "employeeAllocation",
            "employeeAllocation.branch",
            "employeeAllocation.teams",
            "employeeAllocation.positions",
            "employeeAllocation.shiftCycle",
            "employeeAllocation.shiftCycle.shift"
    })
    Page<EmployeeRoster> findAll(Specification<EmployeeRoster> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "employee",
            "holiday",
            "holiday.holidayGroup",
            "employeeAllocation",
            "employeeAllocation.branch",
            "employeeAllocation.teams",
            "employeeAllocation.positions",
            "employeeAllocation.shiftCycle",
            "employeeAllocation.shiftCycle.shift"
    })
    List<EmployeeRoster> findAll(Specification<EmployeeRoster> spec);
}

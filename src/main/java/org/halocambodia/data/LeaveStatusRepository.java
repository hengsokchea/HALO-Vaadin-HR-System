package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveStatusRepository  extends JpaRepository<LeaveStatus, Long>, JpaSpecificationExecutor<LeaveStatus> {
	
    @Query("SELECT ls FROM LeaveStatus ls WHERE ls.id IN (1, 4) AND ls.isActive = true ORDER BY ls.sortOrder")
    List<LeaveStatus> findPersonalLeaveStatuses();
    
    @Query("SELECT ls FROM LeaveStatus ls WHERE ls.id IN (2, 3, 8) AND ls.isActive = true ORDER BY ls.sortOrder")
    List<LeaveStatus> findSupervisorLeaveStatuses();
    
    @Query("SELECT ls FROM LeaveStatus ls WHERE ls.id IN (5, 6, 7, 9) AND ls.isActive = true ORDER BY ls.sortOrder")
    List<LeaveStatus> findFinalLeaveStatuses();
    
    @Query("SELECT ls FROM LeaveStatus ls WHERE ls.isActive = true ORDER BY ls.sortOrder")
    List<LeaveStatus> findAllActive();
    
    @Query("SELECT ls FROM LeaveStatus ls WHERE ls.id = :id AND ls.isActive = true")
    Optional<LeaveStatus> findActiveById(@Param("id") Long id);

}

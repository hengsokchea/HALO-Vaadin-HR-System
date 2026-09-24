package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LeaveTypeSubTypeRepository  extends JpaRepository<LeaveTypeSubType, Long>, JpaSpecificationExecutor<LeaveTypeSubType> {
	List<LeaveTypeSubType> findByLeaveTypeIdOrderByLeaveSubTypeNameEnAsc(Long leaveTypeId);

}

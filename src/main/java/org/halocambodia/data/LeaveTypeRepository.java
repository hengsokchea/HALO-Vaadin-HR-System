package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;

public interface LeaveTypeRepository  extends JpaRepository<LeaveType, Long>, JpaSpecificationExecutor<LeaveType> {
	List<LeaveType> findByLeaveTypeGroup(LeaveTypeGroup leaveTypeGroup);
	List<LeaveType> findByLeaveTypeGroupId(Long groupId);
	
	List<LeaveType> findByLeaveTypeGroupIdAndObsoleteDateIsNull(Long groupId);
	
	List<LeaveType> findByLeaveTypeGroupIdAndIdNotIn(Long groupId, List<Long> ids);
	List<LeaveType> findByObsoleteDateIsNullOrderByLeavTypeCodeAsc();

	@EntityGraph(attributePaths = {"leaveTypeGroup", "leaveTypeSubTypes"})
	Optional<LeaveType> findOneById(Long id);
}

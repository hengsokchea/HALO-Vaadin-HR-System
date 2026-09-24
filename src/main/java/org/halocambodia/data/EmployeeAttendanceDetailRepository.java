package org.halocambodia.data;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

public interface EmployeeAttendanceDetailRepository  extends JpaRepository<EmployeeAttendanceDetail, Long>, JpaSpecificationExecutor<EmployeeAttendanceDetail> {

	Optional<EmployeeAttendanceDetail> findFirstByEmployeeIdAndAttendanceDateOrderByIdDesc(
			Long employeeId, LocalDate attendanceDate);

	@Override
	@EntityGraph(attributePaths = {
			"employee",
			"leaveType",
			"leaveType.leaveTypeGroup",
			"leaveTypeSubType",
			"reportedByEmployee",
			"reportedByPosition",
			"reportLocation",
			"qcByEmployee",
			"qcByPosition",
			"qcLocation",
			"userCreated",
			"userUpdated"
	})
	Page<EmployeeAttendanceDetail> findAll(
			Specification<EmployeeAttendanceDetail> specification,
			Pageable pageable);

	@Query("""
			select distinct attendance
			from EmployeeAttendanceDetail attendance
			left join fetch attendance.attendanceDetailTeams teamRow
			left join fetch teamRow.team
			where attendance.id in :attendanceIds
			""")
	List<EmployeeAttendanceDetail> fetchTeamsForAttendanceIds(
			@Param("attendanceIds") Collection<Long> attendanceIds);

	@Query("""
			select distinct teamRow
			from EmployeeAttendanceDetailTeam teamRow
			left join fetch teamRow.teamTasks taskRow
			left join fetch taskRow.task
			where teamRow.attendanceDetail.id in :attendanceIds
			""")
	List<EmployeeAttendanceDetailTeam> fetchTasksForAttendanceIds(
			@Param("attendanceIds") Collection<Long> attendanceIds);

	@EntityGraph(attributePaths = {
			"employee", "employee.positions", "leaveType", "leaveTypeSubType",
			"reportLocation",
			"dataEntryBy", "dataEntryPosition", "dataEntryLocation",
			"qcByEmployee", "qcByPosition", "qcLocation", "qcReturnBy"
	})
	@Query("""
			select attendance from EmployeeAttendanceDetail attendance
			where attendance.attendanceDate between :startDate and :endDate
			  and attendance.entryStatus = org.halocambodia.enums.AttendanceEntryStatus.SUBMITTED
			  and attendance.reportLocation.id in :allowedBranchIds
			order by attendance.employee.insuranceNo, attendance.attendanceDate, attendance.id
			""")
	List<EmployeeAttendanceDetail> findForQcVerification(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("allowedBranchIds") Collection<Long> allowedBranchIds);

	@EntityGraph(attributePaths = {
			"employee", "employee.positions", "leaveType", "leaveTypeSubType",
			"reportLocation",
			"dataEntryBy", "dataEntryPosition", "dataEntryLocation",
			"qcByEmployee", "qcByPosition", "qcLocation",
			"hrVerificationBy", "hrVerificationPosition"
	})
	@Query("""
			select attendance from EmployeeAttendanceDetail attendance
			where attendance.attendanceDate between :startDate and :endDate
			  and attendance.entryStatus = org.halocambodia.enums.AttendanceEntryStatus.SUBMITTED
			  and attendance.qcStatus = org.halocambodia.enums.AttendanceQcStatus.VERIFIED
			  and attendance.qcByEmployee is not null
			  and attendance.reportLocation.id in :allowedBranchIds
			order by attendance.employee.insuranceNo, attendance.attendanceDate, attendance.id
			""")
	List<EmployeeAttendanceDetail> findForHrFinalReview(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("allowedBranchIds") Collection<Long> allowedBranchIds);

	@EntityGraph(attributePaths = {
			"employee", "employee.positions", "leaveType", "leaveTypeSubType",
			"reportLocation",
			"dataEntryBy", "dataEntryPosition", "dataEntryLocation",
			"qcByEmployee", "qcByPosition", "qcLocation", "qcReturnBy",
			"hrVerificationBy", "hrVerificationPosition"
	})
	@Query("""
			select attendance from EmployeeAttendanceDetail attendance
			where attendance.attendanceDate between :startDate and :endDate
			  and attendance.reportLocation.id in :allowedBranchIds
			order by attendance.employee.insuranceNo,
			         attendance.attendanceDate,
			         attendance.id
			""")
	List<EmployeeAttendanceDetail> findForAttendanceSummary(
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("allowedBranchIds") Collection<Long> allowedBranchIds);

	@EntityGraph(attributePaths = { "employee", "reportLocation" })
	@Query("""
			select attendance
			from EmployeeAttendanceDetail attendance
			where attendance.id in :attendanceIds
			""")
	List<EmployeeAttendanceDetail> findForQcActionByIds(
			@Param("attendanceIds") Collection<Long> attendanceIds);

	@Query("""
			select attendance.employee.id, attendance.attendanceDate
			from EmployeeAttendanceDetail attendance
			where attendance.employee.id in :employeeIds
			  and attendance.attendanceDate between :startDate and :endDate
			group by attendance.employee.id, attendance.attendanceDate
			having count(attendance.id) > 1
			""")
	List<Object[]> findDuplicateEmployeeDates(
			@Param("employeeIds") Collection<Long> employeeIds,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// Kept for backward compatibility with older attendance services.
	long countByEmployeeIdAndAttendanceDate(
			Long employeeId, LocalDate attendanceDate);
}

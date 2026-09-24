package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;

import org.halocambodia.data.*;
import org.halocambodia.enums.AttendanceEntryStatus;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.role_management.RoleManagementView;
import org.halocambodia.views.attendance_management.AttendanceRegisterView;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.halocambodia.views.leave_management.LeaveRequestView;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class EmployeeAttendanceDetailService implements GenericService<EmployeeAttendanceDetail> {
		
	  private final EmployeeAttendanceDetailRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	  private final EmployeeRepository employeeRepository;
	  private final PayrollAttendanceLockService attendanceLockService;
	  
	  @PersistenceContext
	  private EntityManager entityManager;

	  public EmployeeAttendanceDetailService(EmployeeAttendanceDetailRepository repository,
	          AuthenticatedUser authenticatedUser, EmployeeRepository employeeRepository,
	          PayrollAttendanceLockService attendanceLockService) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        this.employeeRepository = employeeRepository;
	        this.attendanceLockService = attendanceLockService;
	        
	  }
	  
		  @Override
		  @Transactional
		  public Page<EmployeeAttendanceDetail> list(Pageable pageable, Specification<EmployeeAttendanceDetail> filter) {
			  Page<EmployeeAttendanceDetail> page = repository.findAll(filter, pageable);
			  List<Long> attendanceIds = page.getContent().stream()
					  .map(EmployeeAttendanceDetail::getId)
					  .filter(java.util.Objects::nonNull)
					  .toList();

			  if (!attendanceIds.isEmpty()) {
				  // Load the two collection levels in two bounded queries. Fetching both
				//  List bags in one query would cause MultipleBagFetchException.
				  repository.fetchTeamsForAttendanceIds(attendanceIds);
				  repository.fetchTasksForAttendanceIds(attendanceIds);
			  }

			  return page;
		  }
	    
	  @Override
	  public long count(Specification<EmployeeAttendanceDetail> filter) {
	        Specification<EmployeeAttendanceDetail> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  @Transactional
	  public void delete(Set<EmployeeAttendanceDetail> entities) {
	      if (!authenticatedUser.hasPage(DailyAttendanceView.class, AccessPageType.DELETED_PAGE)) {
	          throw new IllegalArgumentException("You don't have permission to do this operation");
	      }

	      for (EmployeeAttendanceDetail leave : entities) {
	    	  EmployeeAttendanceDetail managed = repository.findById(leave.getId())
	                  .orElseThrow(() -> new IllegalArgumentException("Leave not found"));
	          attendanceLockService.requireEditable(managed.getAttendanceDate());

	          // Ensure children are loaded
	          managed.getAttendanceDetailTeams().size();

	          repository.delete(managed); // ✅ Hibernate cascades delete to details
	      }
	  }

	  @Override
	  @Transactional
	  public EmployeeAttendanceDetail update(EmployeeAttendanceDetail incoming) {
	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));
	      
	      boolean isUpdating = incoming.getId() != null;

		     
	      // Validate permissions
	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(DailyAttendanceView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }

	      boolean isNew = incoming.getId() == null;
	      EmployeeAttendanceDetail managed = isNew ? new EmployeeAttendanceDetail() : repository.findById(incoming.getId()).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
	      if (!isNew) attendanceLockService.requireEditable(managed.getAttendanceDate());
	      attendanceLockService.requireEditable(incoming.getAttendanceDate());

	      // copy simple fields
	      managed.setReportedByEmployee(incoming.getReportedByEmployee());
	      managed.setReportedByPosition(incoming.getReportedByPosition());
	      managed.setReportedDate(incoming.getReportedDate());
	      managed.setReportLocation(incoming.getReportLocation());

	      Employee defaultDataEntryEmployee = resolveCurrentEmployee(currentUserLogin);
	      managed.setDataEntryBy(incoming.getDataEntryBy() != null
	              ? incoming.getDataEntryBy() : defaultDataEntryEmployee);
	      managed.setDataEntryPosition(incoming.getDataEntryPosition() != null
	              ? incoming.getDataEntryPosition() : managed.getDataEntryBy().getPositions());
	      managed.setDataEntryLocation(incoming.getDataEntryLocation() != null
	              ? incoming.getDataEntryLocation() : managed.getDataEntryBy().getBranch());
	      managed.setDataEntryDate(incoming.getDataEntryDate() != null
	              ? incoming.getDataEntryDate() : java.time.LocalDate.now());
	      managed.setEntryStatus(incoming.getEntryStatus() != null
	              ? incoming.getEntryStatus() : AttendanceEntryStatus.DRAFT);
	      
	      managed.setEmployee(incoming.getEmployee());
	      managed.setAttendanceDate(incoming.getAttendanceDate());
	      managed.setLeaveType(incoming.getLeaveType());
	      managed.setLeaveTypeSubType(incoming.getLeaveTypeSubType());
	      managed.setLeaveDuration(incoming.getLeaveDuration());
	      managed.setFirstIn(incoming.getFirstIn());
	      managed.setLastOut(incoming.getLastOut());
	      managed.setBreakMinutes(incoming.getBreakMinutes());
	      managed.setTotalHours(incoming.getTotalHours());
	      managed.setNormalHours(incoming.getNormalHours());
	      managed.setOvertimeHours(incoming.getOvertimeHours());
	      
	      managed.setSurvey123Id(incoming.getSurvey123Id());
	      managed.setRemark(incoming.getRemark());
	      

	      if (isNew) managed.setUserCreated(currentUserLogin);
	      managed.setUserUpdated(currentUserLogin);

	      // re-attach details
	      managed.getAttendanceDetailTeams().clear();
	      if (incoming.getAttendanceDetailTeams() != null) {
	          for (EmployeeAttendanceDetailTeam detail : incoming.getAttendanceDetailTeams()) {
	              detail.setAttendanceDetail(managed);
	    	      if (detail.getId()==null) detail.setUserCreated(currentUserLogin);
	    	      detail.setUserUpdated(currentUserLogin);
	    	      
	              managed.getAttendanceDetailTeams().add(detail);
	          }
	      }

	      // Hibernate cascades automatically due to CascadeType.ALL
	      return entityManager.merge(managed);
	  }

	  private Employee resolveCurrentEmployee(User user) {
	      String insurance = user.getInsurance();
	      if (insurance == null || insurance.isBlank()) {
	          throw new IllegalArgumentException("Current user is not linked to an employee. "
	                  + "| អ្នកប្រើប្រាស់បច្ចុប្បន្នមិនបានភ្ជាប់ជាមួយបុគ្គលិកទេ។");
	      }
	      try {
	          return employeeRepository.findByInsuranceNo(Integer.valueOf(insurance.trim()))
	                  .orElseThrow(() -> new IllegalArgumentException(
	                          "Employee for current user was not found."));
	      } catch (NumberFormatException ex) {
	          throw new IllegalArgumentException("Current user's insurance number is invalid.");
	      }
	  }

	  @Transactional
	  public int submitSelected(Collection<Long> attendanceIds) {
	      if (!authenticatedUser.hasPage(DailyAttendanceView.class, AccessPageType.UPDATED_PAGE)) {
	          throw new IllegalArgumentException("You don't have permission to submit attendance. "
	                  + "| អ្នកមិនមានសិទ្ធិបញ្ជូនវត្តមានទេ។");
	      }
	      if (attendanceIds == null || attendanceIds.isEmpty()) return 0;

	      User currentUser = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));
	      Employee dataEntryEmployee = resolveCurrentEmployee(currentUser);
	      List<EmployeeAttendanceDetail> records = repository.findAllById(attendanceIds);
	      attendanceLockService.requireAllEditable(records.stream()
	              .map(EmployeeAttendanceDetail::getAttendanceDate)
	              .toList());
	      int submitted = 0;

	      for (EmployeeAttendanceDetail record : records) {
	          if (record.getEntryStatus()
	                  == AttendanceEntryStatus.SUBMITTED) continue;
	          record.setDataEntryBy(dataEntryEmployee);
	          record.setDataEntryPosition(dataEntryEmployee.getPositions());
	          record.setDataEntryLocation(dataEntryEmployee.getBranch());
	          record.setDataEntryDate(java.time.LocalDate.now());
	          record.setEntryStatus(AttendanceEntryStatus.SUBMITTED);
	          record.setQcStatus(org.halocambodia.enums.AttendanceQcStatus.PENDING);
	          record.setQcByEmployee(null);
	          record.setQcByPosition(null);
	          record.setQcLocation(null);
	          record.setQcDate(null);
	          record.setUserUpdated(currentUser);
	          submitted++;
	      }

	      repository.saveAll(records);
	      return submitted;
	  }

	  
	    public List<EmployeeAttendanceDetail> findAll(Specification<EmployeeAttendanceDetail> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    


}

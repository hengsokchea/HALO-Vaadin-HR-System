package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.role_management.RoleManagementView;
import org.halocambodia.views.attendance_management.AttendanceRegisterView;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.halocambodia.views.goal_setting.GoalSettingEntryView;
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
public class EmployeeAttendanceDetailRegisterService implements GenericService<EmployeeAttendanceDetail> {
		
	  private final EmployeeAttendanceDetailRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	  private final PayrollAttendanceLockService attendanceLockService;
	  
	  @PersistenceContext
	  private EntityManager entityManager;

	  public EmployeeAttendanceDetailRegisterService(EmployeeAttendanceDetailRepository repository,
	          AuthenticatedUser authenticatedUser,
	          PayrollAttendanceLockService attendanceLockService) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        this.attendanceLockService = attendanceLockService;
	        
	  }
	  
	  @Override
	  public Page<EmployeeAttendanceDetail> list(Pageable pageable, Specification<EmployeeAttendanceDetail> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EmployeeAttendanceDetail> filter) {
	        Specification<EmployeeAttendanceDetail> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  @Transactional
	  public void delete(Set<EmployeeAttendanceDetail> entities) {
	      if (!authenticatedUser.hasPage(AttendanceRegisterView.class, AccessPageType.DELETED_PAGE)) {
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
	      if (!authenticatedUser.hasPage(AttendanceRegisterView.class, accessPageType)) {
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
	      
	      managed.setQcByEmployee(incoming.getQcByEmployee());
	      managed.setQcByPosition(incoming.getQcByPosition());
	      managed.setQcLocation(incoming.getQcLocation());
	      managed.setQcDate(incoming.getQcDate());
	      managed.setSurvey123Id(incoming.getSurvey123Id());
	      managed.setRemark(incoming.getRemark());
	      
	      managed.setHrVerificationBy(incoming.getHrVerificationBy());
	      managed.setHrVerificationPosition(incoming.getHrVerificationPosition());
	      managed.setHrVerificationDate(incoming.getHrVerificationDate());
	      

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

	  
	    public List<EmployeeAttendanceDetail> findAll(Specification<EmployeeAttendanceDetail> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    


}

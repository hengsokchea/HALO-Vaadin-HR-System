package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.role_management.RoleManagementView;
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
public class EmployeeAttendanceService implements GenericService<EmployeeAttendance> {
	
	
	  private final EmployeeAttendanceRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	  
	  @PersistenceContext
	  private EntityManager entityManager;



	  public EmployeeAttendanceService(EmployeeAttendanceRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        
	  }
	  
	  @Override
	  public Page<EmployeeAttendance> list(Pageable pageable, Specification<EmployeeAttendance> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EmployeeAttendance> filter) {
	        Specification<EmployeeAttendance> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  @Transactional
	  public void delete(Set<EmployeeAttendance> entities) {
	      if (!authenticatedUser.hasPage(DailyAttendanceView.class, AccessPageType.DELETED_PAGE)) {
	          throw new IllegalArgumentException("You don't have permission to do this operation");
	      }

	      for (EmployeeAttendance leave : entities) {
	    	  EmployeeAttendance managed = repository.findById(leave.getId())
	                  .orElseThrow(() -> new IllegalArgumentException("Leave not found"));

	          // Ensure children are loaded
	          managed.getAttendanceDetail().size();

	          repository.delete(managed); // ✅ Hibernate cascades delete to details
	      }
	  }

	  @Override
	  @Transactional
	  public EmployeeAttendance update(EmployeeAttendance incoming) {
	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isNew = incoming.getId() == null;
	      EmployeeAttendance managed = isNew ? new EmployeeAttendance() : repository.findById(incoming.getId()).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

	      // copy simple fields
	      managed.setReportedByEmployee(incoming.getReportedByEmployee());
	      managed.setReportedByPosition(incoming.getReportedByPosition());
	      managed.setReportedDate(incoming.getReportedDate());
	      managed.setReportLocation(incoming.getReportLocation());
	      
	      managed.setQcByEmployee(incoming.getQcByEmployee());
	      managed.setQcByPosition(incoming.getQcByPosition());
	      managed.setQcLocation(incoming.getQcLocation());
	      managed.setQcDate(incoming.getQcDate());
	      managed.setSurvey123Id(incoming.getSurvey123Id());
	      

	      if (isNew) managed.setUserCreated(currentUserLogin);
	      managed.setUserUpdated(currentUserLogin);

	      // re-attach details
	      managed.getAttendanceDetail().clear();
	      if (incoming.getAttendanceDetail() != null) {
	          for (EmployeeAttendanceDetail detail : incoming.getAttendanceDetail()) {
	              detail.setAttendance(managed);
	    	      if (detail.getId()==null) detail.setUserCreated(currentUserLogin);
	    	      detail.setUserUpdated(currentUserLogin);
	    	      
	              managed.getAttendanceDetail().add(detail);
	          }
	      }

	      // Hibernate cascades automatically due to CascadeType.ALL
	      return entityManager.merge(managed);
	  }

	  
	    public List<EmployeeAttendance> findAll(Specification<EmployeeAttendance> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    


}

package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.role_management.RoleManagementView;
import org.halocambodia.views.attendance_management.AttendanceRegisterView;
import org.halocambodia.views.leave_management.LeaveRequestView;
import org.halocambodia.views.leave_management.SupervisorLeaveReviewView;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class EmployeeLeaveSupervisorService implements GenericService<EmployeeLeave> {
	
	
	  private final EmployeeLeaveRepository repository;
	  private final AttachmentRepository attachmentRepository;
	  private final AuthenticatedUser authenticatedUser;
	  
	  @PersistenceContext
	  private EntityManager entityManager;



	  public EmployeeLeaveSupervisorService(EmployeeLeaveRepository repository,AuthenticatedUser authenticatedUser,AttachmentRepository attachmentRepository) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        this.attachmentRepository=attachmentRepository;
	        
	  }
	  
	  @Override
	  public Page<EmployeeLeave> list(Pageable pageable, Specification<EmployeeLeave> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EmployeeLeave> filter) {
	        Specification<EmployeeLeave> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  @Transactional
	  public void delete(Set<EmployeeLeave> entities) {
	      if (!authenticatedUser.hasPage(SupervisorLeaveReviewView.class, AccessPageType.DELETED_PAGE)) {
	          throw new IllegalArgumentException("You don't have permission to do this operation");
	      }

	      for (EmployeeLeave leave : entities) {
	          EmployeeLeave managed = repository.findById(leave.getId())
	                  .orElseThrow(() -> new IllegalArgumentException("Leave not found"));

	          // Ensure children are loaded
	          managed.getEmployeeLeaveDetails().size();
	          
	          // Load and delete associated attachments
	          List<Attachment> attachments = attachmentRepository.findByEntity(managed);
	          if (attachments != null && !attachments.isEmpty()) {
	              attachmentRepository.deleteAll(attachments);
	          }

	          repository.delete(managed); // ✅ Hibernate cascades delete to details
	      }
	  }

	  @Override
	  @Transactional
	  public EmployeeLeave update(EmployeeLeave incoming) {
	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));
	      
	      boolean isUpdating = incoming.getId() != null;
	      // Validate permissions
	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(SupervisorLeaveReviewView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }

	      boolean isNew = incoming.getId() == null;
	      EmployeeLeave managed = isNew ? new EmployeeLeave() : repository.findById(incoming.getId()).orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

	      // copy simple fields
	      managed.setRequestDate(incoming.getRequestDate());
	      managed.setEmployee(incoming.getEmployee());
	      managed.setDepartment(incoming.getDepartment());
	      managed.setPositions(incoming.getPositions());
	      managed.setLineManager(incoming.getLineManager());
	      managed.setLineManagerPositions(incoming.getLineManagerPositions());
	      managed.setReason(incoming.getReason());
	      managed.setContractNumber(incoming.getContractNumber());
	      managed.setLeaveStatus(incoming.getLeaveStatus());

	      
	      managed.setAnnualLeaveAvailableBalance(incoming.getAnnualLeaveAvailableBalance());
	      managed.setSickLeaveUsed(incoming.getSickLeaveUsed());
	      managed.setSpecialLeaveUsed(incoming.getSpecialLeaveUsed());
	      managed.setUnpaidLeaveUsed(incoming.getUnpaidLeaveUsed());
	      managed.setPaternityLeaveUsed(incoming.getPaternityLeaveUsed());
	      managed.setMaternityLeaveUsed(incoming.getMaternityLeaveUsed());
	      managed.setCompensatoryLeaveRemaining(incoming.getCompensatoryLeaveRemaining());
	      
	      managed.setLineManagerCheckDate(incoming.getLineManagerCheckDate());
	      managed.setLineManagerChecked(incoming.getLineManagerChecked());
	      managed.setLineManagerCheckedPositions(incoming.getLineManagerCheckedPositions());
	      managed.setLineManagerCheckComments(incoming.getLineManagerCheckComments());
	      managed.setLineManagerCheckedStatus(incoming.getLineManagerCheckedStatus());
	      
	      managed.setHrVerificationDate(incoming.getHrVerificationDate());
	      managed.setHrVerificationBy(incoming.getHrVerificationBy());
	      managed.setHrVerificationPosition(incoming.getHrVerificationPosition());
	      managed.setHrVerificationComments(incoming.getHrVerificationComments());
	      managed.setHrVerificationStatus(incoming.getHrVerificationStatus());

	      if (isNew) managed.setUserCreated(currentUserLogin);
	      managed.setUserUpdated(currentUserLogin);

	      // re-attach details
	      managed.getEmployeeLeaveDetails().clear();
	      if (incoming.getEmployeeLeaveDetails() != null) {
	          for (EmployeeLeaveDetail detail : incoming.getEmployeeLeaveDetails()) {
	              detail.setEmployeeLeave(managed);
	    	      if (detail.getId()==null) detail.setUserCreated(currentUserLogin);
	    	      detail.setUserUpdated(currentUserLogin);
	    	      
	              managed.getEmployeeLeaveDetails().add(detail);
	          }
	      }

	      // Hibernate cascades automatically due to CascadeType.ALL
	      return entityManager.merge(managed);
	  }

	  
	    public List<EmployeeLeave> findAll(Specification<EmployeeLeave> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    

	    @Transactional
	    public void saveAttachment(Attachment attachment) {
	        attachmentRepository.save(attachment);
	    }

	    public List<Attachment> findAttachmentsByEntityAndType(EmployeeLeave leave, Long typeId) {
	        return attachmentRepository.findByEntityAndType(leave, typeId);
	    }



}

package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.EmployeeLeaveBalance;
import org.halocambodia.data.EmployeeLeaveBalanceRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.leave_management.EmployeeLeaveBalanceView;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;



import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class EmployeeLeaveBalanceService implements GenericService<EmployeeLeaveBalance> {
	
	
	  private final EmployeeLeaveBalanceRepository repository;
	  private final AuthenticatedUser authenticatedUser;

	  
	  public EmployeeLeaveBalanceService(EmployeeLeaveBalanceRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;        
	  }
	  
	  @Override
	  public Page<EmployeeLeaveBalance> list(Pageable pageable, Specification<EmployeeLeaveBalance> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EmployeeLeaveBalance> filter) {
	        Specification<EmployeeLeaveBalance> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <EmployeeLeaveBalance> entity) {
		  if (!authenticatedUser.hasPage(EmployeeLeaveBalanceView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public EmployeeLeaveBalance update(EmployeeLeaveBalance entityValue) {
	      // Retrieve the currently logged-in user
	      User currentUserLogin = authenticatedUser.get().orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;
	      Long excludeId = isUpdating ? entityValue.getId() : null;
	      
	      // Set audit fields
	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      // Validate permissions
	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(EmployeeLeaveBalanceView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
	    public List<EmployeeLeaveBalance> findAll(Specification<EmployeeLeaveBalance> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	  /*  
	    public List<EmployeeLeaveBalance> searchEmployeesByNameEnKhInsurance(String filter, Pageable pageable) {
	    	String f = (filter == null) ? "" : filter.trim();
	        return repository.searchEmployeesByNameEnKhInsurance(f, pageable).getContent();
	    }

	    public long countEmployeesByNameEnKhInsurance(String filter) {
	        return repository.countEmployeesByNameEnKhInsurance(filter);
	    }
	*/
}

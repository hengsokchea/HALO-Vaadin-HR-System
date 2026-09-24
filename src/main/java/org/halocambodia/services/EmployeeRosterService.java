package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.halocambodia.views.goal_setting.GoalSettingEntryView;
import org.halocambodia.views.goal_setting.GoalSupervisorView;
import org.halocambodia.views.roster.RosterView;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
//import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeRosterService implements GenericService<EmployeeRoster> {
	
	
	  private final EmployeeRosterRepository repository;
	  private final AuthenticatedUser authenticatedUser;

	  public EmployeeRosterService(EmployeeRosterRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;        
	  }
	
	  /*
	  @Override
	  public Page<EmployeeRoster> list(Pageable pageable, Specification<EmployeeRoster> filter) {
		  return repository.findAll(filter, pageable);
	  }
	*/   
	  /*
	  @Override
	  public long count(Specification<EmployeeRoster> filter) {
	        Specification<EmployeeRoster> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	 */
	  
	  @Override
	  public void delete(Set <EmployeeRoster> entity) {
		  if (!authenticatedUser.hasPage(RosterView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public EmployeeRoster update(EmployeeRoster entityValue) {
	      // Retrieve the currently logged-in user
	      User currentUserLogin = authenticatedUser.get()
	          .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      // Normalize the name for validation
	      //String lowerCaseName = entityValue.getModelName().toLowerCase().trim();
	      
	      // Set audit fields
	      entityValue.setUserCreated(currentUserLogin); // Only if it’s a new record
	      entityValue.setUserUpdated(currentUserLogin);

	      boolean isUpdating = entityValue.getId() != null;

	     
	      // Validate permissions
	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(RosterView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
//	    public List<EmployeeRoster> findAll(Specification<EmployeeRoster> filter){
//	    	return repository.findAll((filter != null) ? filter : null);
//	    }

	    @Override
	    @Transactional(readOnly = true)
	    public Page<EmployeeRoster> list(Pageable pageable, Specification<EmployeeRoster> filter) {
	        return repository.findAll(filter, pageable);
	    }

	    @Override
	    @Transactional(readOnly = true)
	    public long count(Specification<EmployeeRoster> filter) {
	        return repository.count(filter);
	    }

	    @Transactional(readOnly = true)
	    public List<EmployeeRoster> findAll(Specification<EmployeeRoster> filter) {
	        return repository.findAll(filter);
	    }

	    @Transactional(readOnly = true)
	    public Page<EmployeeRoster> findAll(Specification<EmployeeRoster> spec, Pageable pageable) {
	        return repository.findAll(spec, pageable);
	    }

}

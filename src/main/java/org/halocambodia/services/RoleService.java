package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.role_management.RoleManagementView;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class RoleService implements GenericService<Role> {
	
	
	  private final RoleRepository repository;
	  private final AuthenticatedUser authenticatedUser;

	  public RoleService(RoleRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        
	  }
	  
	  @Override
	  public Page<Role> list(Pageable pageable, Specification<Role> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<Role> filter) {
	        Specification<Role> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <Role> entity) {
		  if (!authenticatedUser.hasPage(RoleManagementView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
		    
	       repository.deleteAllInBatch(entity);
	  }
	    

	  @Override
	  @Transactional
	  public Role update(Role entityValue) {
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
	      if (!authenticatedUser.hasPage(RoleManagementView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }

	    public List<Role> findAll(Specification<Role> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    


	    

}

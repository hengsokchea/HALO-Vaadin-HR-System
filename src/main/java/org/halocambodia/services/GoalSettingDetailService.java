package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.halocambodia.views.goal_setting.GoalSettingDetailView;
import org.halocambodia.views.goal_setting.GoalSupervisorView;

import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class GoalSettingDetailService implements GenericService<GoalSettingDetail> {
	
	
	  private final GoalSettingDetailRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	
	  
	  public GoalSettingDetailService(GoalSettingDetailRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        
	  }
	  
	  @Override
	  public Page<GoalSettingDetail> list(Pageable pageable, Specification<GoalSettingDetail> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<GoalSettingDetail> filter) {
	        Specification<GoalSettingDetail> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <GoalSettingDetail> entity) {
		  if (!authenticatedUser.hasPage(GoalSettingDetailView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public GoalSettingDetail update(GoalSettingDetail entityValue) {
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
	      if (!authenticatedUser.hasPage(GoalSettingDetailView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
	    public List<GoalSettingDetail> findAll(Specification<GoalSettingDetail> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    

	    

}

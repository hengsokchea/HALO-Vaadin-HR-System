package org.halocambodia.services;


import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;
import org.halocambodia.security.*;
import org.halocambodia.views.branch.BranchView;
import org.halocambodia.views.policy.PolicyCategoryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class PolicyCategoryService implements GenericService<PolicyCategory> {

	  private final PolicyCategoryRepository repository;
	  
	  private final AuthenticatedUser authenticatedUser;

	  public PolicyCategoryService(PolicyCategoryRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;   
	  }
	  
	  @Transactional(readOnly = true)
	  public Optional<PolicyCategory> findById(Long id) {
		    return repository.findById(id);
	 }
	  
	  @Override
	  @Transactional(readOnly = true)
	  public Page<PolicyCategory> list(Pageable pageable, Specification<PolicyCategory> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  @Transactional(readOnly = true)
	  public long count(Specification<PolicyCategory> filter) {
	        Specification<PolicyCategory> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <PolicyCategory> entities) {
		  
		  if (!authenticatedUser.hasPage(PolicyCategoryView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
		  //This is fine if Branch has no children and no entity lifecycle methods (@PreRemove, @PostRemove).
	       repository.deleteAllInBatch(entities);
		   //repository.deleteAll(entities); 
	  }
	    

	  @Override
	  public PolicyCategory update(PolicyCategory entityValue) {

	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;

	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      
	      if (isUpdating) {
	    	  Optional<PolicyCategory> lastest=this.findById(entityValue.getId());
	          entityValue.setVersion(lastest.get().getVersion());
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      if (!authenticatedUser.hasPage(PolicyCategoryView.class, isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }    

	      return repository.save(entityValue);
	  }

	  @Transactional(readOnly = true)
	  public List<PolicyCategory> findAll(Specification<PolicyCategory> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	  }
	  
 

}

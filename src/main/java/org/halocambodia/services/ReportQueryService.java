package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.*;

import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.halocambodia.views.reports.simple_reports.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class ReportQueryService implements GenericService<ReportQuery> {
	
	
	  private final ReportQueryRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	
	  
	  public ReportQueryService(ReportQueryRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        
	  }
	  
	  @Override
	  public Page<ReportQuery> list(Pageable pageable, Specification<ReportQuery> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<ReportQuery> filter) {
	        Specification<ReportQuery> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <ReportQuery> entity) {
		  if (!authenticatedUser.hasPage(ReportsView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public ReportQuery update(ReportQuery entityValue) {
	      // Retrieve the currently logged-in user
	      User currentUserLogin = authenticatedUser.get()
	          .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      // Normalize the name for validation
	      //String lowerCaseName = entityValue.getReportSlug().toLowerCase().trim();
	      
	      // Set audit fields
	      entityValue.setUserCreated(currentUserLogin); // Only if it’s a new record
	      entityValue.setUserUpdated(currentUserLogin);

	      boolean isUpdating = entityValue.getId() != null;

	      // Check for unique name constraint
	      //long count = isUpdating
	      //    ? repository.countByReportSlugIgnoreCaseAndIdNot(lowerCaseName, entityValue.getId())
	       //   : repository.countByReportSlugIgnoreCase(lowerCaseName);

	     // if (count > 0) {
	    //      throw new IllegalArgumentException(isUpdating
	    //          ? "Update: Location already exists."
//	      }

	      // Validate permissions
	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(ReportsView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
	  public List<ReportQuery> findAll(Specification<ReportQuery> spec) {
		    if (spec == null) {
		        return repository.findAll();
		    }
		    return repository.findAll(spec);
	 }
	    


	    public ReportQuery loadFresh(Long id) {
	        return repository.findById(id)
	            .orElseThrow(() -> new IllegalStateException("Report not found"));
	    }

}

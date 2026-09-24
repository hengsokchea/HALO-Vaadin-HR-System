package org.halocambodia.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.employee.InternationalStaffView;

import org.halocambodia.views.employee.NationalStaffActiveView;
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
public class EmployeeInternationActiveService implements GenericService<Employee> {
	
	
	  private final EmployeeRepository repository;
	  private final AuthenticatedUser authenticatedUser;

	  
	  public EmployeeInternationActiveService(EmployeeRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;        
	  }
	  
	  @Override
	  public Page<Employee> list(Pageable pageable, Specification<Employee> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<Employee> filter) {
	        Specification<Employee> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <Employee> entity) {
		  if (!authenticatedUser.hasPage(InternationalStaffView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public Employee update(Employee entityValue) {
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
	      if (!authenticatedUser.hasPage(InternationalStaffView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
	    public List<Employee> findAll(Specification<Employee> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    
	    public List<Employee> searchEmployeesByNameEnKhInsurance(String filter, Pageable pageable) {
	    	String f = (filter == null) ? "" : filter.trim();
	        return repository.searchEmployeesByNameEnKhInsurance(f, pageable).getContent();
	    }

	    public long countEmployeesByNameEnKhInsurance(String filter) {
	        return repository.countEmployeesByNameEnKhInsurance(filter);
	    }

}

package org.halocambodia.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
import org.halocambodia.data.EducationCenter;
import org.halocambodia.data.EducationCenterRepository;
import org.halocambodia.data.EducationType;
import org.halocambodia.data.EducationTypeRepository;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeePerformanceReview;
import org.halocambodia.data.EmployeePerformanceReviewRating;
import org.halocambodia.data.EmployeePerformanceReviewRepository;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Positions;
import org.halocambodia.data.ReviewCriteria;
import org.halocambodia.data.ReviewCriteriaGroup;
import org.halocambodia.data.ReviewCriteriaGroupRepository;
import org.halocambodia.data.ReviewCriteriaRepository;
import org.halocambodia.data.TrainingCourse;
import org.halocambodia.data.TrainingCourseRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.department.DepartmentView;
import org.halocambodia.views.education.EducationCenterView;
import org.halocambodia.views.education.EducationQualificationsView;
import org.halocambodia.views.employee.NationalStaffActiveView;
import org.halocambodia.views.performance.EmployeePerformanceReviewView;
import org.halocambodia.views.training.TrainingCourseView;
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
public class EducationCenterService implements GenericService<EducationCenter> {
	
	@PersistenceContext
	private EntityManager em;

	
	  private final EducationCenterRepository repository;
	  
	  private final AuthenticatedUser authenticatedUser;


	  
	  public EducationCenterService(EducationCenterRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;   

	  }
	  
	  @Override
	  public Page<EducationCenter> list(Pageable pageable, Specification<EducationCenter> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EducationCenter> filter) {
	        Specification<EducationCenter> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <EducationCenter> entities) {
		  if (!authenticatedUser.hasPage(EducationCenterView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       //repository.deleteAllInBatch(entity);
		  	repository.deleteAll(entities); 
	  }
	    

	  @Override
	  @Transactional
	  public EducationCenter update(EducationCenter entityValue) {

	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;

	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(EducationCenterView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }


	      return repository.save(entityValue);
	  }

	  
	    public List<EducationCenter> findAll(Specification<EducationCenter> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }

}

package org.halocambodia.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.AttachmentType;
import org.halocambodia.data.AttachmentTypeRepository;
import org.halocambodia.data.Department;
import org.halocambodia.data.DepartmentRepository;
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
import org.halocambodia.views.attachment_type.AttachmentTypeView;
import org.halocambodia.views.department.DepartmentView;
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
public class AttachmentTypeService implements GenericService<AttachmentType> {
	
	@PersistenceContext
	private EntityManager em;

	
	  private final AttachmentTypeRepository repository;
	  
	  private final AuthenticatedUser authenticatedUser;


	  
	  public AttachmentTypeService(AttachmentTypeRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;   

	  }
	  
	  @Override
	  public Page<AttachmentType> list(Pageable pageable, Specification<AttachmentType> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<AttachmentType> filter) {
	        Specification<AttachmentType> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <AttachmentType> entities) {
		  if (!authenticatedUser.hasPage(AttachmentTypeView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       //repository.deleteAllInBatch(entity);
		  	repository.deleteAll(entities); 
	  }
	    

	  @Override
	  @Transactional
	  public AttachmentType update(AttachmentType entityValue) {

	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;

	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(AttachmentTypeView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }


	      return repository.save(entityValue);
	  }

	  
	    public List<AttachmentType> findAll(Specification<AttachmentType> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }

}

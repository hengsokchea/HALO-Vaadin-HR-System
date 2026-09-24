package org.halocambodia.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeePerformanceReview;
import org.halocambodia.data.EmployeePerformanceReviewRating;
import org.halocambodia.data.EmployeePerformanceReviewRepository;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.Language;
import org.halocambodia.data.LanguageRepository;
import org.halocambodia.data.PositionRepository;
import org.halocambodia.data.Positions;
import org.halocambodia.data.ReviewCriteria;
import org.halocambodia.data.ReviewCriteriaGroup;
import org.halocambodia.data.ReviewCriteriaGroupRepository;
import org.halocambodia.data.ReviewCriteriaRepository;
import org.halocambodia.data.Teams;
import org.halocambodia.data.TeamsRepository;
import org.halocambodia.data.TrainingCourse;
import org.halocambodia.data.TrainingCourseRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.employee.NationalStaffActiveView;
import org.halocambodia.views.language.LanguageView;
import org.halocambodia.views.performance.EmployeePerformanceReviewView;
import org.halocambodia.views.teams.TeamsView;
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
public class LanguageService implements GenericService<Language> {
	
	@PersistenceContext
	private EntityManager em;

	
	  private final LanguageRepository repository;
	  
	  private final AuthenticatedUser authenticatedUser;


	  
	  public LanguageService(LanguageRepository repository,AuthenticatedUser authenticatedUser) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;   

	  }
	  
	  @Override
	  public Page<Language> list(Pageable pageable, Specification<Language> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<Language> filter) {
	        Specification<Language> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <Language> entities) {
		  if (!authenticatedUser.hasPage(LanguageView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       //repository.deleteAllInBatch(entity);
		  	repository.deleteAll(entities); 
	  }
	    

	  @Override
	  @Transactional
	  public Language update(Language entityValue) {

	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;

	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(LanguageView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }


	      return repository.save(entityValue);
	  }

	  
	    public List<Language> findAll(Specification<Language> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }

}

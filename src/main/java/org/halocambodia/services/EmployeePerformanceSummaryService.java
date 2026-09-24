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
import org.halocambodia.data.ReviewCriteria;
import org.halocambodia.data.ReviewCriteriaGroup;
import org.halocambodia.data.ReviewCriteriaGroupRepository;
import org.halocambodia.data.ReviewCriteriaRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.employee.NationalStaffActiveView;
import org.halocambodia.views.performance.EmployeePerformanceFinalReviewView;
import org.halocambodia.views.performance.EmployeePerformanceReviewView;
import org.halocambodia.views.performance.EmployeePerformanceSummaryView;
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
public class EmployeePerformanceSummaryService implements GenericService<EmployeePerformanceReview> {
	
	@PersistenceContext
	private EntityManager em;

	
	  private final EmployeePerformanceReviewRepository repository;
	  
	  private final AuthenticatedUser authenticatedUser;

	  private final EmployeeRepository employeeRepository;
	  private final ReviewCriteriaGroupRepository groupRepo;
	  private final ReviewCriteriaRepository criteriaRepo;
	  
	  public EmployeePerformanceSummaryService(EmployeePerformanceReviewRepository repository,AuthenticatedUser authenticatedUser, EmployeeRepository employeeRepository,ReviewCriteriaGroupRepository groupRepo,ReviewCriteriaRepository criteriaRepo) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;   
	        this.employeeRepository=employeeRepository;
	        
	        this.groupRepo = groupRepo;
	        this.criteriaRepo = criteriaRepo;
	  }
	  
	  @Override
	  public Page<EmployeePerformanceReview> list(Pageable pageable, Specification<EmployeePerformanceReview> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<EmployeePerformanceReview> filter) {
	        Specification<EmployeePerformanceReview> spec =(filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <EmployeePerformanceReview> entities) {
		  if (!authenticatedUser.hasPage(EmployeePerformanceSummaryView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       //repository.deleteAllInBatch(entity);
		  	repository.deleteAll(entities); 
	  }
	    

	  @Override
	  @Transactional
	  public EmployeePerformanceReview update(EmployeePerformanceReview entityValue) {

	      User currentUserLogin = authenticatedUser.get()
	              .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

	      boolean isUpdating = entityValue.getId() != null;

	      if (!isUpdating) {
	          entityValue.setUserCreated(currentUserLogin);
	      }
	      entityValue.setUserUpdated(currentUserLogin);

	      AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
	      if (!authenticatedUser.hasPage(EmployeePerformanceSummaryView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }

	      // ✅ Ensure ratings have owning side set + attach criteria properly
	      if (entityValue.getRatings() != null) {
	          for (EmployeePerformanceReviewRating r : entityValue.getRatings()) {
	              r.setReview(entityValue);
	              
	              boolean newRow = (r.getId() == null); // or (r.getCreatedAt() == null)
	              if (newRow) {
	                  r.setUserCreated(currentUserLogin);
	              }
	              r.setUserUpdated(currentUserLogin);	              

	              if (r.getCriteria() != null && r.getCriteria().getId() != null) {
	                  Long cid = r.getCriteria().getId();

	                  // ✅ THIS LINE fixes: detached entity + @Version null
	                  r.setCriteria(em.getReference(ReviewCriteria.class, cid));
	              }
	          }
	      }

	      return repository.save(entityValue);
	  }

	  
	    public List<EmployeePerformanceReview> findAll(Specification<EmployeePerformanceReview> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    
	    public List<Employee> searchEmployeesByNameEnKhInsurance(String filter, Pageable pageable) {
	    	String f = (filter == null) ? "" : filter.trim();
	        return employeeRepository.searchEmployeesByNameEnKhInsurance(f, pageable).getContent();
	    }

	    public long countEmployeesByNameEnKhInsurance(String filter) {
	        return employeeRepository.countEmployeesByNameEnKhInsurance(filter);
	    }

	    
	    
	    // =========================================================
	    // ✅ NEW: Load active groups + criteria (sorted)
	    // =========================================================
	    public List<GroupWithCriteria> loadActiveCriteria() {
	        List<ReviewCriteriaGroup> groups = groupRepo.findByIsActiveTrueOrderBySortOrderAscGroupCodeAsc();
	        List<GroupWithCriteria> out = new ArrayList<>();

	        for (ReviewCriteriaGroup g : groups) {
	            List<ReviewCriteria> criteria =
	                    criteriaRepo.findByGroupIdAndIsActiveTrueOrderBySortOrderAscCriteriaCodeAsc(g.getId());
	            out.add(new GroupWithCriteria(g, criteria));
	        }
	        return out;
	    }

	    // DTO (simple)
	    public record GroupWithCriteria(ReviewCriteriaGroup group, List<ReviewCriteria> criteria) {}

	    // =========================================================
	    // ✅ NEW: Ensure review has rating rows for all active criteria
	    // (call this when opening the editor)
	    // =========================================================
	    public void ensureRatingRows(EmployeePerformanceReview review) {

	        if (review.getRatings() == null) {
	            review.setRatings(new ArrayList<>());
	        }

	        Map<Long, EmployeePerformanceReviewRating> byCriteriaId = new LinkedHashMap<>();
	        for (EmployeePerformanceReviewRating r : review.getRatings()) {
	            if (r.getCriteria() != null && r.getCriteria().getId() != null) {
	                byCriteriaId.put(r.getCriteria().getId(), r);
	            }
	        }

	        // add missing rows for active criteria
	        for (GroupWithCriteria gwc : loadActiveCriteria()) {
	            for (ReviewCriteria c : gwc.criteria()) {
	                if (!byCriteriaId.containsKey(c.getId())) {
	                    EmployeePerformanceReviewRating r = new EmployeePerformanceReviewRating();
	                    r.setReview(review);
	                    r.setCriteria(c);
	                    review.getRatings().add(r);
	                    byCriteriaId.put(c.getId(), r);
	                }
	            }
	        }
	    }
}

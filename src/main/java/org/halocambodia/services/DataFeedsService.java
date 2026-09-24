package org.halocambodia.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.admin.user_management.UserManagementView;
import org.halocambodia.views.data_feed.DataFeedView;
import org.halocambodia.views.goal_setting.GoalSupervisorView;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class DataFeedsService implements GenericService<DataFeeds> {
	
	
	  private final DataFeedsRepository repository;
	  private final AuthenticatedUser authenticatedUser;
	  private final DataFeedsLogRepository dataFeedsLogRepository;
	
	  @PersistenceContext
	  private EntityManager entityManager;
	  
	  public DataFeedsService(DataFeedsRepository repository,AuthenticatedUser authenticatedUser,DataFeedsLogRepository dataFeedsLogRepository) {
	        this.repository = repository;
	        this.authenticatedUser = authenticatedUser;
	        this.dataFeedsLogRepository=dataFeedsLogRepository;
	        
	  }
	  
	  @Override
	  public Page<DataFeeds> list(Pageable pageable, Specification<DataFeeds> filter) {
		  return repository.findAll(filter, pageable);
	  }
	    
	  @Override
	  public long count(Specification<DataFeeds> filter) {
	        Specification<DataFeeds> spec = (filter != null) ? filter : null;
	        return repository.count(spec);
	  }
	  
	  @Override
	  public void delete(Set <DataFeeds> entity) {
		  if (!authenticatedUser.hasPage(DataFeedView.class, AccessPageType.DELETED_PAGE)) {
			  throw new IllegalArgumentException("You don't have permission to do this operation ");
	       }
	    	 
	       repository.deleteAllInBatch(entity);
	  }
	    

	    
	    

	  @Override
	  @Transactional
	  public DataFeeds update(DataFeeds entityValue) {
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
	      if (!authenticatedUser.hasPage(DataFeedView.class, accessPageType)) {
	          throw new IllegalArgumentException("You don't have permission to perform this operation");
	      }
	      // Save the entity
	      return repository.save(entityValue);
	  }
	  
	    public List<DataFeeds> findAll(Specification<DataFeeds> filter){
	    	return repository.findAll((filter != null) ? filter : null);
	    }
	    
	    public List<DataFeedsLog> pageLatestGroups(long feedId, String mode, int offset, int limit) {
	        return dataFeedsLogRepository.findLatestPerGroup(feedId, mode, limit, offset);
	    }
	    
	    public int countLatestGroups(long feedId, String mode) {
	        return Math.toIntExact(dataFeedsLogRepository.countGroups(feedId, mode));
	    }

	    
	    public long countOccurrences(Long feedId, String mode, UUID parentPrimaryKey, Long selfIdWhenNoParent) {
	      
	         if (parentPrimaryKey != null) {
	             return dataFeedsLogRepository.countByParentKey(feedId, parentPrimaryKey);
	         } else {
	             return dataFeedsLogRepository.countBySelfId(feedId, selfIdWhenNoParent);
	         }
	     }
	    
	    /**
	     * Count distinct parent keys with errors for a given feed
	     * This is used by the UI to show error counts in the grid
	     * 
	     * @param feedId The data feed ID
	     * @return Number of distinct parent keys with errors
	     */
	    @Transactional(readOnly = true)
	    public int countErrorGroups(Long feedId) {
	        try {
	            // Use native query for efficiency - counts distinct parent keys with errors
	            String sql = """
	                SELECT COUNT(DISTINCT COALESCE(parent_primary_key::text, data_feed_log_id::text))
	                FROM core_system.data_feed_log
	                WHERE data_feed_id = :feedId
	                AND has_errors = true
	            """;
	            
	            Query query = entityManager.createNativeQuery(sql);
	            query.setParameter("feedId", feedId);
	            
	            Number result = (Number) query.getSingleResult();
	            return result != null ? result.intValue() : 0;
	        } catch (Exception e) {
	            // Log error and return 0 to avoid breaking the UI
	            System.err.println("Error counting error groups for feed " + feedId + ": " + e.getMessage());
	            return 0;
	        }
	    }
	    
	    /**
	     * Get error count with parent key grouping for a specific feed
	     * Alternative implementation using repository count method
	     */
	    @Transactional(readOnly = true)
	    public int countErrorGroupsViaRepository(Long feedId) {
	        try {
	            // Alternative: count using repository with specification
	            // This is less efficient than native query but still works
	            Long count = dataFeedsLogRepository.countErrorsByFeedId(feedId);
	            return count != null ? count.intValue() : 0;
	        } catch (Exception e) {
	            return 0;
	        }
	    }
	    
	    public void loadErrorCounts(List<DataFeeds> feeds) {

	        Map<Long,Integer> counts =
	            repository.getErrorCounts()
	                .stream()
	                .collect(Collectors.toMap(
	                    r -> ((Number) r[0]).longValue(),
	                    r -> ((Number) r[1]).intValue()
	                ));

	        feeds.forEach(feed ->
	            feed.setErrorCount(
	                counts.getOrDefault(feed.getId(), 0)
	            )
	        );
	    }

}
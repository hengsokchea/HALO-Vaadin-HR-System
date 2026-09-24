package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewCriteriaRepository  extends JpaRepository<ReviewCriteria, Long>, JpaSpecificationExecutor<ReviewCriteria> {
	
	List<ReviewCriteria> findByGroupIdAndIsActiveTrueOrderBySortOrderAscCriteriaCodeAsc(Long groupId);
	List<ReviewCriteria> findByIsActiveTrue(org.springframework.data.domain.Sort sort);

}

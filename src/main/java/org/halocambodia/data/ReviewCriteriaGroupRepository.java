package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReviewCriteriaGroupRepository  extends JpaRepository<ReviewCriteriaGroup, Long>, JpaSpecificationExecutor<ReviewCriteriaGroup> {
	
	
	List<ReviewCriteriaGroup> findByIsActiveTrueOrderBySortOrderAscGroupCodeAsc();
}

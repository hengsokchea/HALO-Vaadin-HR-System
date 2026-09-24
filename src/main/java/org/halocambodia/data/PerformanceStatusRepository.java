package org.halocambodia.data;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PerformanceStatusRepository extends JpaRepository<PerformanceStatus, Long>,JpaSpecificationExecutor<PerformanceStatus> {
	
	List<PerformanceStatus> findByStatusNameIn(Collection<String> codes);
	
	Optional<PerformanceStatus> findByStatusNameIgnoreCase(String statusName);



}

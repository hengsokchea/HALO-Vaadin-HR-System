package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BranchRepository  extends JpaRepository<Branch, Long>, JpaSpecificationExecutor<Branch> {
	Optional<Branch> findByBranchShortName(String name);
	Optional<Branch> findByBranchFullNameIgnoreCase(String name);
	
	List<Branch> findByIsActiveTrue();
	
	List<Branch> findByBranchFullNameInIgnoreCase(Collection<String> branchNames);

}

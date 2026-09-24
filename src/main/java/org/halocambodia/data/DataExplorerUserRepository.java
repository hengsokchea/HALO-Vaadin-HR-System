package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataExplorerUserRepository  extends JpaRepository<DataExplorerUser, Long>, JpaSpecificationExecutor<DataExplorerUser> {
	boolean existsByUserAndDataExplorer(User user, DataExplorer dataExplorer);
	
	@Query("SELECT COALESCE(MAX(deu.sortOrder), 0) FROM DataExplorerUser deu WHERE deu.user = :user")
	int findMaxSortOrderByUser(@Param("user") User user);
	
	List<DataExplorerUser> findByUserOrderBySortOrder(User user);




}

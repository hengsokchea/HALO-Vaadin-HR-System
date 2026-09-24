package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CategoryRepository  extends JpaRepository<Category, String>, JpaSpecificationExecutor<Category> {
	Optional<Category> findByCategoryIgnoreCase(String name);
	
	List<Category> findByCategoryInIgnoreCase(Collection<String> categoryNames);
}

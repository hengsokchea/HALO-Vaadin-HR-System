package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PoGradeRepository  extends JpaRepository<PoGrade, String>, JpaSpecificationExecutor<PoGrade> {
	Optional<PoGrade> findByPoGradeIgnoreCase(String name);
	
	List<PoGrade> findByPoGradeInIgnoreCase(Collection<String> poGradeNames);
}

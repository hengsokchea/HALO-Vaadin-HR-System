package org.halocambodia.data;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UnitRepository  extends JpaRepository<Unit, Long>, JpaSpecificationExecutor<Unit> {
	Optional<Unit> findByUnitName(String name);

	
	

}

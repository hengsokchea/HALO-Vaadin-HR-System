package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PositionRepository  extends JpaRepository<Positions, Long>, JpaSpecificationExecutor<Positions> {

	 Optional<Positions> findByPositionIgnoreCase(String position);
	 
	 List<Positions> findByPositionInIgnoreCase(Collection<String> positionNames);
	 
	 List<Positions> findByObsolateDateIsNull();
}

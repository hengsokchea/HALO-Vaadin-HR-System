package org.halocambodia.data;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleRepository extends JpaRepository<Role, Long>  , JpaSpecificationExecutor<Role>{
    // Additional query methods can be added here
	Optional<Role> findByName(String name);
}

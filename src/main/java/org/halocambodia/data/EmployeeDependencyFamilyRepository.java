package org.halocambodia.data;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmployeeDependencyFamilyRepository extends JpaRepository<EmployeeDependencyFamily, Long>, JpaSpecificationExecutor<EmployeeDependencyFamily> {

}


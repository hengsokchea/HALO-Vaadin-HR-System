package org.halocambodia.data;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeSupervisionRepository  extends JpaRepository<EmployeeSupervision, Long>, JpaSpecificationExecutor<EmployeeSupervision> {

}

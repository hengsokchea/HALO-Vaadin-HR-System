package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeEmergencyContactRepository  extends JpaRepository<EmployeeEmergencyContact, Long>, JpaSpecificationExecutor<EmployeeEmergencyContact> {

}

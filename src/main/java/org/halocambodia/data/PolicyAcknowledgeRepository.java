package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyAcknowledgeRepository extends JpaRepository<PolicyAcknowledge, Long>, JpaSpecificationExecutor<PolicyAcknowledge> {

    Optional<PolicyAcknowledge> findByEmployeeAndPolicy(Employee employee, Policy policy);

    List<PolicyAcknowledge> findByEmployee(Employee employee);

    List<PolicyAcknowledge> findByPolicy(Policy policy);

    List<PolicyAcknowledge> findByEmployeeId(Long employeeId);

    List<PolicyAcknowledge> findByPolicyId(Long policyId);

    boolean existsByEmployeeAndPolicy(Employee employee, Policy policy);

    long countByPolicy(Policy policy);

    long countByEmployee(Employee employee);
    
    Optional<PolicyAcknowledge> findByEmployeeAndPolicyAndPolicyVersion(
            Employee employee, Policy policy, Long policyVersion);


}
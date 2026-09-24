package org.halocambodia.data;

import java.util.List;

import org.halocambodia.data.PolicyAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PolicyAccessLogRepository       extends JpaRepository<PolicyAccessLog, Long>,     JpaSpecificationExecutor<PolicyAccessLog> {

    @Override
    @EntityGraph(attributePaths = {
            "policy",
            "user",
            "fileAttachment"
    })
    Page<PolicyAccessLog> findAll(
            Specification<PolicyAccessLog> specification,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "policy",
            "user",
            "fileAttachment"
    })
    List<PolicyAccessLog> findAll(
            Specification<PolicyAccessLog> specification);

    List<PolicyAccessLog> findTop500ByOrderByAccessedAtDesc();

    List<PolicyAccessLog> findByPolicyIdOrderByAccessedAtDesc(Long policyId);
}

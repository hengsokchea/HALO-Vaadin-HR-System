package org.halocambodia.services;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.halocambodia.data.DateTimeUtilFormart;
import org.halocambodia.data.Employee;
import org.halocambodia.data.Policy;
import org.halocambodia.data.PolicyAccessAction;
import org.halocambodia.data.PolicyAcknowledge;
import org.halocambodia.data.PolicyAcknowledgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyAcknowledgeService {

    private static final Logger log =LoggerFactory.getLogger(PolicyAcknowledgeService.class);

    private final PolicyAcknowledgeRepository repository;
    private final CurrentEmployeeService currentEmployeeService;
    private final PolicyAccessLogService accessLogService;

    @Transactional(readOnly = true)
    public Optional<PolicyAcknowledge> findCurrent( Policy policy) {
    	
        if (policy == null  || policy.getId() == null  || policy.getVersion() == null) {
            return Optional.empty();
        }
        Employee employee = currentEmployeeService.requireEmployee();
        return repository.findByEmployeeAndPolicyAndPolicyVersion( employee, policy, policy.getVersion() );
    }

    @Transactional(readOnly = true)
    public boolean hasAcknowledgedCurrentVersion( Policy policy) {
        return findCurrent(policy).isPresent();
    }

    @Transactional(readOnly = true)
    public List<PolicyAcknowledge> findMine() {

        Employee employee = currentEmployeeService.requireEmployee();
        return repository.findByEmployee(employee);
    }

    @Transactional
    public PolicyAcknowledge acknowledge( Policy policy,HttpServletRequest request) {

        validatePolicy(policy);
        var currentUser =currentEmployeeService.requireUser();

        Employee employee = currentEmployeeService.requireEmployee();
        PolicyAcknowledge acknowledgement = repository.findByEmployeeAndPolicyAndPolicyVersion(employee,policy,policy.getVersion()).orElseGet(PolicyAcknowledge::new);

        boolean newRecord =acknowledgement.getId() == null;
        acknowledgement.setPolicy(policy);
        acknowledgement.setEmployee(employee);
        acknowledgement.setPolicyVersion(policy.getVersion());
        acknowledgement.setAcknowledgedAt(ZonedDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE));
        acknowledgement.setIpAddress(accessLogService.resolveIp(request));
        acknowledgement.setDeviceName(accessLogService.resolveUserAgent(request));
        if (newRecord) {
            acknowledgement.setUserCreated(currentUser);
        }
        acknowledgement.setUserUpdated(currentUser);
        acknowledgement.setUpdatedAt(ZonedDateTime.now(DateTimeUtilFormart.CAMBODIA_ZONE));
        PolicyAcknowledge saved =repository.save(acknowledgement);
        try {
            accessLogService.record(policy, PolicyAccessAction.ACKNOWLEDGED,null,request);
        } catch (Exception exception) {
            log.warn("Acknowledgement was saved, but access logging failed. "  + "Policy ID: {}, employee ID: {}",  policy.getId(),employee.getId(), exception);
        }
        return saved;
    }

    private void validatePolicy(Policy policy) {
    	
        if (policy == null || policy.getId() == null) {
            throw new IllegalArgumentException("Policy is required.");
        }
        if (policy.getVersion() == null) {
            throw new IllegalArgumentException("Policy version is required.");
        }
    }
}
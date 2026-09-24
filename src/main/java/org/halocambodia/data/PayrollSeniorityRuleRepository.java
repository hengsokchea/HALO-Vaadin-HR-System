package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PayrollSeniorityRuleRepository
        extends JpaRepository<PayrollSeniorityRule, Long>,
                JpaSpecificationExecutor<PayrollSeniorityRule> {

    List<PayrollSeniorityRule> findAllByOrderByEffectiveFromDesc();
}

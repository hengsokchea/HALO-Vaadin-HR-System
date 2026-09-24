package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface PayrollPolicyRuleRepository
        extends JpaRepository<PayrollPolicyRule, Long>, JpaSpecificationExecutor<PayrollPolicyRule> {

    List<PayrollPolicyRule> findAllByOrderByRuleYearDescSortOrderAscRuleCodeAsc();

    List<PayrollPolicyRule> findByRuleYearOrderBySortOrderAscRuleCodeAsc(Integer ruleYear);

    Optional<PayrollPolicyRule> findByRuleYearAndRuleCodeIgnoreCase(Integer ruleYear, String ruleCode);

    boolean existsByPayrollComponentId(Long payrollComponentId);

    @Query("select distinct rule.ruleYear from PayrollPolicyRule rule order by rule.ruleYear desc")
    List<Integer> findDistinctRuleYears();

    @Query("""
            select count(rule)
              from PayrollPolicyRule rule
             where rule.ruleYear = :ruleYear
               and rule.active = true
               and rule.ruleType = 'OVERTIME'
               and rule.calculationSource = 'ATTENDANCE'
               and upper(coalesce(rule.rateUnit, '')) = 'HOUR'
               and (:excludedId is null or rule.id <> :excludedId)
            """)
    long countOtherActiveHourlyOvertimeRules(
            @Param("ruleYear") Integer ruleYear,
            @Nullable @Param("excludedId") Long excludedId);

    @Query("""
            select rule.ruleCode
              from PayrollPolicyRule rule
             where rule.ruleYear = :ruleYear
               and rule.active = true
               and rule.ruleType = 'LEAVE_PAY'
               and rule.calculationSource = 'LEAVE'
               and (:excludedId is null or rule.id <> :excludedId)
            """)
    List<String> findOtherActiveLeavePayCodes(
            @Param("ruleYear") Integer ruleYear,
            @Nullable @Param("excludedId") Long excludedId);
}

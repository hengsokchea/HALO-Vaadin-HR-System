package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollSeniorityCalculationRepository
        extends JpaRepository<PayrollSeniorityCalculation, Long> {

    @EntityGraph(attributePaths = {"payrollEmployee", "seniorityRule"})
    @Query("""
            SELECT calculation
            FROM PayrollSeniorityCalculation calculation
            WHERE calculation.seniorityRule.id = :ruleId
            ORDER BY calculation.seniorityYear DESC,
                     calculation.semesterNo DESC,
                     calculation.payrollEmployee.insuranceNo ASC
            """)
    List<PayrollSeniorityCalculation> findResults(@Param("ruleId") Long ruleId);

    @EntityGraph(attributePaths = {"payrollEmployee", "seniorityRule"})
    @Query("""
            SELECT calculation
            FROM PayrollSeniorityCalculation calculation
            WHERE calculation.payrollEmployee.payrollRunId = :runId
              AND (:payrollEmployeeId IS NULL
                   OR calculation.payrollEmployee.id = :payrollEmployeeId)
            ORDER BY calculation.seniorityYear DESC,
                     calculation.semesterNo DESC,
                     calculation.payrollEmployee.insuranceNo ASC
            """)
    List<PayrollSeniorityCalculation> findByPayrollRun(
            @Param("runId") Long runId,
            @Param("payrollEmployeeId") Long payrollEmployeeId);

    long countBySeniorityRuleId(Long seniorityRuleId);
}

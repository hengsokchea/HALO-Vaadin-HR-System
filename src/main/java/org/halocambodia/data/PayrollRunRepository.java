package org.halocambodia.data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollRunRepository
        extends JpaRepository<PayrollRun, Long>, JpaSpecificationExecutor<PayrollRun> {

    List<PayrollRun> findByPayrollPeriodIdOrderByRunNumberDesc(Long payrollPeriodId);

    Optional<PayrollRun> findTopByPayrollPeriodIdOrderByRunNumberDesc(Long payrollPeriodId);

    Optional<PayrollRun> findTopByPayrollPeriodIdAndRunTypeAndStatusOrderByRunNumberDesc(
            Long payrollPeriodId, String runType, String status);

    long countByPayrollPeriodId(Long payrollPeriodId);

    long countByPayrollPeriodIdAndStatusIn(
            Long payrollPeriodId, Collection<String> statuses);

    long countByPayrollPeriodIdAndRunTypeAndStatusNot(
            Long payrollPeriodId, String runType, String excludedStatus);

    long countByPayrollPeriodIdAndRunTypeAndStatusIn(
            Long payrollPeriodId, String runType, Collection<String> statuses);

    long countByPayrollPeriodIdAndRunTypeAndStatusNotIn(
            Long payrollPeriodId, String runType, Collection<String> statuses);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PayrollRun run
               set run.status = 'DRAFT',
                   run.calculatedAt = null,
                   run.calculatedBy = null,
                   run.version = run.version + 1
             where run.payrollPeriodId = :periodId
               and run.status = 'CALCULATED'
            """)
    int resetCalculatedRunsForPeriod(@Param("periodId") Long periodId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update PayrollRun run
               set run.status = 'DRAFT',
                   run.calculatedAt = null,
                   run.calculatedBy = null,
                   run.version = run.version + 1
             where run.status = 'CALCULATED'
            """)
    int resetAllCalculatedRuns();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_run run
               SET status = 'DRAFT',
                   calculated_at = NULL,
                   calculated_by = NULL,
                   version = run.version + 1
             WHERE run.status = 'CALCULATED'
               AND run.payroll_period_id IN (
                   SELECT period.payroll_period_id
                     FROM public.payroll_period period
                     JOIN public.payroll_tax_config config
                       ON config.tax_year = period.payroll_year
                    WHERE config.payroll_tax_config_id = :taxConfigId
                      AND config.active = TRUE
               )
            """, nativeQuery = true)
    int resetCalculatedRunsForTaxConfig(@Param("taxConfigId") Long taxConfigId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_run run
               SET status = 'DRAFT',
                   calculated_at = NULL,
                   calculated_by = NULL,
                   version = run.version + 1
              FROM public.payroll_period period,
                   public.payroll_nssf_config config
             WHERE run.payroll_period_id = period.payroll_period_id
               AND config.payroll_nssf_config_id = :nssfConfigId
               AND config.active = TRUE
               AND run.status = 'CALCULATED'
               AND period.payment_date >= config.effective_from
               AND (config.effective_to IS NULL
                    OR period.payment_date <= config.effective_to)
            """, nativeQuery = true)
    int resetCalculatedRunsForNssfConfig(@Param("nssfConfigId") Long nssfConfigId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_run run
               SET status = 'DRAFT',
                   calculated_at = NULL,
                   calculated_by = NULL,
                   version = run.version + 1
              FROM public.payroll_period period
             WHERE run.payroll_period_id = period.payroll_period_id
               AND run.status = 'CALCULATED'
               AND period.payroll_year = :ruleYear
            """, nativeQuery = true)
    int resetCalculatedRunsForRuleYear(@Param("ruleYear") Integer ruleYear);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_run run
               SET status = 'DRAFT',
                   calculated_at = NULL,
                   calculated_by = NULL,
                   version = run.version + 1
              FROM public.payroll_period period
             WHERE run.payroll_period_id = period.payroll_period_id
               AND run.status = 'CALCULATED'
               AND period.payment_date >= :effectiveFrom
               AND (CAST(:effectiveTo AS date) IS NULL
                    OR period.payment_date <= CAST(:effectiveTo AS date))
            """, nativeQuery = true)
    int resetCalculatedRunsForNssfRange(
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

}

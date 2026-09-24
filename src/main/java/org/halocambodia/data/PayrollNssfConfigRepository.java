package org.halocambodia.data;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

public interface PayrollNssfConfigRepository
        extends JpaRepository<PayrollNssfConfig, Long>, JpaSpecificationExecutor<PayrollNssfConfig> {

    List<PayrollNssfConfig> findAllByOrderByEffectiveFromDesc();

    @Query(value = """
            SELECT config.*
              FROM public.payroll_nssf_config config
              LEFT JOIN public.contract_type contract
                ON contract.contract_type_id = config.eligible_contract_type_id
             ORDER BY config.effective_from DESC,
                      contract.contract_type_name NULLS FIRST,
                      config.payroll_nssf_config_id DESC
            """, nativeQuery = true)
    List<PayrollNssfConfig> findAllForAdministration();

    @Query(value = """
            SELECT COUNT(*)
              FROM public.payroll_nssf_config config
             WHERE config.active = TRUE
               AND (CAST(:configId AS BIGINT) IS NULL
                    OR config.payroll_nssf_config_id <> :configId)
               AND config.eligible_contract_type_id IS NOT DISTINCT FROM
                   CAST(:contractTypeId AS BIGINT)
               AND config.effective_from <= COALESCE(CAST(:effectiveTo AS DATE), DATE '9999-12-31')
               AND COALESCE(config.effective_to, DATE '9999-12-31') >= :effectiveFrom
            """, nativeQuery = true)
    long countActiveOverlaps(
            @Nullable @Param("configId") Long configId,
            @Param("contractTypeId") Long contractTypeId,
            @Nullable @Param("effectiveTo") LocalDate effectiveTo,
            @Param("effectiveFrom") LocalDate effectiveFrom);

    @Query(value = """
            SELECT COUNT(*)
              FROM public.payroll_period period
              JOIN public.payroll_nssf_config config
                ON period.payment_date >= config.effective_from
               AND (config.effective_to IS NULL
                    OR period.payment_date <= config.effective_to)
             WHERE config.payroll_nssf_config_id = :configId
            """, nativeQuery = true)
    long countCoveredPayrollPeriods(@Param("configId") Long configId);

    @Query(value = """
            WITH period_context AS (
                SELECT payroll_period_id, period_start, period_end, payment_date
                  FROM public.payroll_period
                 WHERE payroll_period_id = :periodId
            ),
            employee_contracts AS (
                SELECT DISTINCT master.contract_type_id,
                       COALESCE(contract.contract_type_name,
                                'Contract type not assigned') AS contract_type_name
                  FROM period_context period
                  JOIN public.emp_roster roster
                    ON roster.roster_date BETWEEN period.period_start AND period.period_end
                  JOIN public.emp_master master
                    ON master.emp_id = roster.emp_id
                  LEFT JOIN public.contract_type contract
                    ON contract.contract_type_id = master.contract_type_id
            ),
            resolved AS (
                SELECT employee_contract.*,
                       selected.payroll_nssf_config_id,
                       selected.currency,
                       selected.band_count
                  FROM employee_contracts employee_contract
                  CROSS JOIN period_context period
                  LEFT JOIN LATERAL (
                      SELECT config.payroll_nssf_config_id,
                             config.currency,
                             (SELECT COUNT(*)
                                FROM public.payroll_nssf_wage_band band
                               WHERE band.payroll_nssf_config_id =
                                     config.payroll_nssf_config_id) AS band_count
                        FROM public.payroll_nssf_config config
                       WHERE config.active = TRUE
                         AND period.payment_date IS NOT NULL
                         AND period.payment_date >= config.effective_from
                         AND (config.effective_to IS NULL
                              OR period.payment_date <= config.effective_to)
                         AND config.eligible_contract_type_id = employee_contract.contract_type_id
                       ORDER BY config.effective_from DESC,
                                config.payroll_nssf_config_id DESC
                       LIMIT 1
                  ) selected ON TRUE
            )
            SELECT DISTINCT
                   CASE
                       WHEN period.payment_date IS NULL THEN
                           'Payment date is required for NSSF'
                       WHEN UPPER(resolved.currency) <> 'KHR' THEN
                           'NSSF rule for ' || resolved.contract_type_name
                               || ' must use KHR currency'
                       WHEN resolved.band_count = 0 THEN
                           'NSSF rule for ' || resolved.contract_type_name
                               || ' has no wage bands'
                   END AS issue
              FROM resolved
              CROSS JOIN period_context period
             WHERE period.payment_date IS NULL
                OR (resolved.payroll_nssf_config_id IS NOT NULL
                    AND (UPPER(resolved.currency) <> 'KHR'
                         OR resolved.band_count = 0))
             ORDER BY issue
            """, nativeQuery = true)
    List<String> findConfigurationIssues(@Param("periodId") Long periodId);
}

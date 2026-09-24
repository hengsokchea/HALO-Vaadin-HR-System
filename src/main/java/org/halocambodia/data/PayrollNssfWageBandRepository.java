package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollNssfWageBandRepository
        extends JpaRepository<PayrollNssfWageBand, Long>, JpaSpecificationExecutor<PayrollNssfWageBand> {

    List<PayrollNssfWageBand> findByPayrollNssfConfigIdOrderByBandOrderAsc(Long payrollNssfConfigId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_nssf_wage_band
               SET band_order = band_order + 1000000,
                   version = version + 1
             WHERE payroll_nssf_config_id = :nssfConfigId
            """, nativeQuery = true)
    int shiftOrdersForReplace(@Param("nssfConfigId") Long nssfConfigId);
}

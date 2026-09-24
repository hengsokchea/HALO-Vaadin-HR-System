package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollTaxBracketRepository
        extends JpaRepository<PayrollTaxBracket, Long>, JpaSpecificationExecutor<PayrollTaxBracket> {

    List<PayrollTaxBracket> findByPayrollTaxConfigIdOrderByBracketOrderAsc(Long payrollTaxConfigId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE public.payroll_tax_bracket
               SET bracket_order = bracket_order + 1000000,
                   version = version + 1
             WHERE payroll_tax_config_id = :taxConfigId
            """, nativeQuery = true)
    int shiftOrdersForReplace(@Param("taxConfigId") Long taxConfigId);
}

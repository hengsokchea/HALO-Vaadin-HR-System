package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PayrollPeriodRepository
        extends JpaRepository<PayrollPeriod, Long>, JpaSpecificationExecutor<PayrollPeriod> {

    List<PayrollPeriod> findAllByOrderByPayrollYearDescPayrollMonthDesc();

    Optional<PayrollPeriod> findByPayrollYearAndPayrollMonth(Integer payrollYear, Integer payrollMonth);

    long countByPayrollYear(Integer payrollYear);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayrollPeriod p where p.id = :periodId")
    Optional<PayrollPeriod> findByIdForUpdate(@Param("periodId") Long periodId);
}

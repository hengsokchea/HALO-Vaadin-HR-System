package org.halocambodia.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollPaymentBatchRepository extends JpaRepository<PayrollPaymentBatch, Long> {

    List<PayrollPaymentBatch> findByPayrollPeriodIdOrderByPaymentDateAscIdAsc(Long payrollPeriodId);

    Optional<PayrollPaymentBatch>
            findTopByPayrollPeriodIdAndInstallmentTypeAndStatusNotOrderByIdDesc(
                    Long payrollPeriodId,
                    String installmentType,
                    String excludedStatus);

    Optional<PayrollPaymentBatch>
            findTopByPayrollRunIdAndInstallmentTypeAndStatusNotOrderByIdDesc(
                    Long payrollRunId,
                    String installmentType,
                    String excludedStatus);

    long countByPayrollPeriodIdAndInstallmentTypeAndStatusIn(
            Long payrollPeriodId,
            String installmentType,
            Collection<String> statuses);

    boolean existsByPayrollPeriodIdAndInstallmentTypeAndStatusNot(
            Long payrollPeriodId,
            String installmentType,
            String excludedStatus);

    boolean existsByPayrollRunIdAndInstallmentTypeAndStatus(
            Long payrollRunId,
            String installmentType,
            String status);
}

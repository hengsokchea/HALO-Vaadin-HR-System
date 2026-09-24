package org.halocambodia.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollEmployeePaymentRepository extends JpaRepository<PayrollEmployeePayment, Long> {

    List<PayrollEmployeePayment> findByPayrollPaymentBatchIdOrderByEmployeeIdAsc(Long batchId);

    long countByPayrollPaymentBatchId(Long batchId);
}

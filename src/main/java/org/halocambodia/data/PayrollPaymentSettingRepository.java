package org.halocambodia.data;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayrollPaymentSettingRepository extends
        JpaRepository<PayrollPaymentSetting, Long>,
        JpaSpecificationExecutor<PayrollPaymentSetting> {

    /**
     * Only one active company payment rule may cover a date range.
     * The optional shift_id belongs to that rule as the SEMI_MONTHLY exception
     * when the company frequency is MONTHLY; it is not a separate rule scope.
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM public.payroll_payment_setting setting
            WHERE setting.emp_id IS NULL
              AND setting.active = TRUE
              AND setting.payroll_payment_setting_id <> :settingId
              AND setting.effective_from <= :effectiveTo
              AND COALESCE(setting.effective_to, DATE '9999-12-31') >= :effectiveFrom
            """, nativeQuery = true)
    long countOverlappingActiveSettings(
            @Param("settingId") Long settingId,
            @Param("effectiveFrom") LocalDate effectiveFrom,
            @Param("effectiveTo") LocalDate effectiveTo);

    /** Any active company rule counts, whether its optional exception Shift is blank or selected. */
    @Query(value = """
            SELECT COUNT(*)
            FROM public.payroll_payment_setting setting
            WHERE setting.emp_id IS NULL
              AND setting.active = TRUE
              AND setting.effective_from <= :paymentDate
              AND (setting.effective_to IS NULL OR setting.effective_to >= :paymentDate)
            """, nativeQuery = true)
    long countActiveCompanySettingsForDate(
            @Param("paymentDate") LocalDate paymentDate);
}

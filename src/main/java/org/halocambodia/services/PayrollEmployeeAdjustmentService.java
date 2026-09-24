package org.halocambodia.services;

import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanInput;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.LoanRepaymentRow;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.RecurringInput;
import static org.halocambodia.data.PayrollEmployeeAdjustmentModels.RecurringRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeRepository;
import org.halocambodia.data.PayrollEmployeeAdjustmentRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.payroll.PayrollView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Employee-level recurring payroll configuration plus loan/recovery management. */
@Service
public class PayrollEmployeeAdjustmentService {

    public static final String FIXED_INCOME = "FIXED_INCOME";
    public static final String OTHER_DEDUCTION = "OTHER_DEDUCTION";
    public static final String COMPANY_LOAN_REPAYMENT = "COMPANY_LOAN_REPAYMENT";

    public static final Set<String> RECURRING_COMPONENT_CODES =
            Set.of(FIXED_INCOME, OTHER_DEDUCTION);
    public static final Set<String> RECURRENCE_TYPES = Set.of("MONTHLY", "ONE_TIME");
    public static final Set<String> LOAN_EDITABLE_STATUSES =
            Set.of("ACTIVE", "PAUSED", "CANCELLED");

    public static final String RECOVERY_COMPANY_LOAN = "COMPANY_LOAN";
    public static final String RECOVERY_ASSET_DAMAGE = "ASSET_DAMAGE";
    public static final String RECOVERY_ASSET_LOSS = "ASSET_LOSS";
    public static final String RECOVERY_OTHER = "OTHER_RECOVERY";

    public static final Set<String> RECOVERY_TYPES = Set.of(
            RECOVERY_COMPANY_LOAN,
            RECOVERY_ASSET_DAMAGE,
            RECOVERY_ASSET_LOSS,
            RECOVERY_OTHER);

    private final PayrollEmployeeAdjustmentRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AuthenticatedUser authenticatedUser;

    public PayrollEmployeeAdjustmentService(
            PayrollEmployeeAdjustmentRepository repository,
            EmployeeRepository employeeRepository,
            AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional(readOnly = true)
    public List<RecurringRow> findRecurring(String search) {
        return repository.findRecurring(search);
    }

    @Transactional(readOnly = true)
    public List<LoanRow> findLoans(String search) {
        return repository.findLoans(search);
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentRow> findLoanRepayments(Long loanId) {
        return repository.findLoanRepayments(loanId);
    }

    @Transactional(readOnly = true)
    public List<Employee> findActiveEmployees() {
        return employeeRepository.findActiveLocalAndInternationalStaff().stream()
                .sorted(java.util.Comparator.comparing(
                        Employee::getInsuranceNo,
                        java.util.Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional
    public Long saveRecurring(RecurringInput input) {
        requirePermission(input == null || input.id() == null
                ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
        validateRecurring(input);

        Long userId = currentUserId();
        repository.ensureSystemComponents(userId);
        Long componentId = repository.findComponentId(input.componentCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Payroll component " + input.componentCode() + " is not active."));

        if (input.id() == null) {
            return repository.insertRecurring(
                    input.employeeId(), componentId, money(input.amount()),
                    normalize(input.recurrenceType()), input.effectiveFrom(), input.effectiveTo(),
                    input.active(), blankToNull(input.description()), blankToNull(input.remarks()), userId);
        }

        if ("ONE_TIME".equals(normalize(input.recurrenceType()))
                && repository.countSettledApplications(input.id()) > 0) {
            throw new IllegalStateException(
                    "This one-time payroll item was already paid and is immutable. Create a new one-time item instead. "
                            + "| ធាតុបើកម្តងនេះបានបើកប្រាក់រួចហើយ។ សូមបង្កើតធាតុថ្មី។");
        }

        int updated = repository.updateRecurring(
                input.id(), input.employeeId(), componentId, money(input.amount()),
                normalize(input.recurrenceType()), input.effectiveFrom(), input.effectiveTo(),
                input.active(), blankToNull(input.description()), blankToNull(input.remarks()), userId);
        if (updated != 1) {
            throw new IllegalStateException("Recurring payroll item changed or no longer exists. Refresh and try again.");
        }
        return input.id();
    }

    @Transactional
    public void removeRecurring(Long id) {
        requirePermission(AccessPageType.DELETED_PAGE);
        if (id == null) return;
        Long userId = currentUserId();
        if (repository.countSettledApplications(id) > 0) {
            repository.deactivateRecurring(id, userId);
            return;
        }
        if (repository.deleteRecurring(id) != 1) {
            // A reserved application can exist in a calculated run. Deactivation
            // preserves the audit link without deleting payroll history.
            repository.deactivateRecurring(id, userId);
        }
    }

    @Transactional
    public Long saveLoan(LoanInput input) {
        requirePermission(input == null || input.id() == null
                ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
        validateLoan(input);

        String recoveryType = normalizeRecoveryType(input.recoveryType());
        String reference = requiredText(input.loanReference(), "Reference is required.");
        if (repository.loanReferenceExists(reference, input.id())) {
            throw new IllegalArgumentException(
                    "Reference already exists. | លេខយោងនេះមានរួចហើយ។");
        }

        BigDecimal principal = money(input.principalAmount());
        BigDecimal installment = money(input.monthlyInstallment());
        Long userId = currentUserId();
        repository.ensureSystemComponents(userId);

        if (input.id() == null) {
            String status = normalizeLoanStatus(input.status());
            if ("SETTLED".equals(status)) status = "ACTIVE";
            return repository.insertLoan(
                    input.employeeId(), recoveryType, reference, principal, installment,
                    input.startDate(), input.endDate(), status,
                    blankToNull(input.remarks()), userId);
        }

        if (repository.countReservedLoanRepayments(input.id()) > 0) {
            throw new IllegalStateException(
                    "This loan is already reserved in a calculated payroll. Recalculate/cancel that payroll before changing the loan. "
                            + "| ប្រាក់កម្ចីនេះត្រូវបានកត់ទុកក្នុងប្រាក់បៀវត្សដែលបានគណនា។ សូមកែដំណើរការប្រាក់បៀវត្សជាមុន។");
        }

        BigDecimal settledAmount = repository.settledLoanAmount(input.id());
        if (principal.compareTo(settledAmount) < 0) {
            throw new IllegalArgumentException(
                    "Principal cannot be lower than the amount already repaid ("
                            + settledAmount + ").");
        }
        BigDecimal outstanding = principal.subtract(settledAmount).max(BigDecimal.ZERO);
        String status = normalizeLoanStatus(input.status());
        if (outstanding.signum() == 0) {
            status = "SETTLED";
        } else if ("SETTLED".equals(status)) {
            throw new IllegalArgumentException(
                    "A loan can be SETTLED only when the outstanding balance is zero.");
        }

        int updated = repository.updateLoan(
                input.id(), input.employeeId(), recoveryType, reference, principal, installment,
                outstanding, input.startDate(), input.endDate(), status,
                blankToNull(input.remarks()), userId);
        if (updated != 1) {
            throw new IllegalStateException("Loan/recovery changed or no longer exists. Refresh and try again.");
        }
        return input.id();
    }

    private static void validateRecurring(RecurringInput input) {
        if (input == null || input.employeeId() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        String code = normalize(input.componentCode());
        if (!RECURRING_COMPONENT_CODES.contains(code)) {
            throw new IllegalArgumentException("Choose FIXED_INCOME or OTHER_DEDUCTION.");
        }
        if (input.amount() == null || input.amount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        String recurrence = normalize(input.recurrenceType());
        if (!RECURRENCE_TYPES.contains(recurrence)) {
            throw new IllegalArgumentException("Recurrence must be MONTHLY or ONE_TIME.");
        }
        if (input.effectiveFrom() == null) {
            throw new IllegalArgumentException("Effective From is required.");
        }
        if (input.effectiveTo() != null && input.effectiveTo().isBefore(input.effectiveFrom())) {
            throw new IllegalArgumentException("Effective To cannot be before Effective From.");
        }
    }

    private static void validateLoan(LoanInput input) {
        if (input == null || input.employeeId() == null) {
            throw new IllegalArgumentException("Employee is required.");
        }
        normalizeRecoveryType(input.recoveryType());
        if (input.principalAmount() == null || input.principalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total loan/recovery amount must be greater than zero.");
        }
        if (input.monthlyInstallment() == null || input.monthlyInstallment().signum() <= 0) {
            throw new IllegalArgumentException("Monthly deduction must be greater than zero.");
        }
        if (input.startDate() == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (input.endDate() != null && input.endDate().isBefore(input.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        normalizeLoanStatus(input.status());
    }

    private void requirePermission(AccessPageType type) {
        if (!authenticatedUser.hasPage(PayrollView.class, type)) {
            throw new IllegalArgumentException("You don't have permission to perform this payroll operation.");
        }
    }

    private Long currentUserId() {
        User user = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in."));
        return user.getId();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }


    private static String normalizeRecoveryType(String value) {
        String type = normalize(value);
        if (type.isBlank()) type = RECOVERY_COMPANY_LOAN;
        if (!RECOVERY_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "Recovery type must be COMPANY_LOAN, ASSET_DAMAGE, ASSET_LOSS, or OTHER_RECOVERY.");
        }
        return type;
    }

    private static String normalizeLoanStatus(String value) {
        String status = normalize(value);
        if (status.isBlank()) status = "ACTIVE";
        if (!Set.of("ACTIVE", "PAUSED", "SETTLED", "CANCELLED").contains(status)) {
            throw new IllegalArgumentException("Invalid loan status.");
        }
        return status;
    }

    private static String requiredText(String value, String message) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new IllegalArgumentException(message);
        return normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

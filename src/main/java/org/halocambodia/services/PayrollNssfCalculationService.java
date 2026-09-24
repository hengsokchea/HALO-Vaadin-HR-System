package org.halocambodia.services;

import java.math.BigDecimal;
import java.util.List;

import org.halocambodia.data.PayrollComponent;
import org.halocambodia.data.PayrollComponentRepository;
import org.halocambodia.data.PayrollNssfCalculationRepository;
import org.halocambodia.data.PayrollNssfCalculationRepository.NssfContext;
import org.halocambodia.data.PayrollNssfWageBand;
import org.halocambodia.data.PayrollNssfWageBandRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** NSSF calculation orchestration without direct JDBC access. */
@Service
public class PayrollNssfCalculationService {

    private final PayrollNssfCalculationRepository calculationRepository;
    private final PayrollNssfWageBandRepository wageBandRepository;
    private final PayrollComponentRepository componentRepository;

    public PayrollNssfCalculationService(
            PayrollNssfCalculationRepository calculationRepository,
            PayrollNssfWageBandRepository wageBandRepository,
            PayrollComponentRepository componentRepository) {
        this.calculationRepository = calculationRepository;
        this.wageBandRepository = wageBandRepository;
        this.componentRepository = componentRepository;
    }

    @Transactional
    public void calculate(
            Long runId,
            @Nullable Long payrollEmployeeId,
            Long userId) {
        NssfContext context = calculationRepository.findContext(runId);
        if (context == null) {
            throw new IllegalArgumentException("Payroll run not found.");
        }
        if (context.paymentDate() == null) {
            throw new IllegalStateException(
                    "Payroll payment date is required for NSSF calculation. "
                            + "| ត្រូវមានថ្ងៃបើកប្រាក់សម្រាប់ការគណនា ប.ស.ស.។");
        }

        List<String> coverageIssues = calculationRepository.findCoverageIssues(
                runId,
                payrollEmployeeId);
        if (!coverageIssues.isEmpty()) {
            throw new IllegalStateException(
                    String.join("; ", coverageIssues.stream().limit(8).toList())
                            + " | សូមកំណត់ច្បាប់ ប.ស.ស. តាមប្រភេទកិច្ចសន្យា "
                            + "មុនគណនាប្រាក់បៀវត្ស។");
        }

        List<Long> applicableConfigIds = calculationRepository.findApplicableConfigIds(
                runId,
                payrollEmployeeId);

        calculationRepository.deleteExistingItems(runId, payrollEmployeeId);

        // No matching contract type means this employee/run is not subject to NSSF.
        if (applicableConfigIds.isEmpty()) {
            return;
        }

        if (!"USD".equals(context.payrollCurrency())) {
            throw new IllegalStateException(
                    "Automatic NSSF currently requires payroll currency USD because payroll earnings are stored in USD. "
                            + "| ការគណនា ប.ស.ស. ដោយស្វ័យប្រវត្តិបច្ចុប្បន្នតម្រូវឱ្យរូបិយប័ណ្ណបើកប្រាក់បៀវត្សជា USD "
                            + "ព្រោះប្រាក់ចំណូលត្រូវបានរក្សាទុកជា USD។");
        }
        if (context.nssfUsdToKhrRate() == null || context.nssfUsdToKhrRate().signum() <= 0) {
            throw new IllegalStateException(
                    "NSSF USD to KHR exchange rate must be greater than zero for NSSF calculation. "
                            + "| អត្រាប្តូរប្រាក់ពី USD ទៅ KHR ត្រូវតែធំជាងសូន្យសម្រាប់ការគណនា ប.ស.ស.។");
        }

        for (Long configId : applicableConfigIds) {
            validateNssfBandConfiguration(configId);
        }

        Long healthEmployeeComponentId = requireSystemComponent(
                "NSSF_HEALTH_EMPLOYEE", "DEDUCTION");
        Long healthEmployerComponentId = requireSystemComponent(
                "NSSF_HEALTH_EMPLOYER", "EMPLOYER_CONTRIBUTION");
        Long riskEmployeeComponentId = requireSystemComponent(
                "NSSF_RISK_EMPLOYEE", "DEDUCTION");
        Long riskEmployerComponentId = requireSystemComponent(
                "NSSF_RISK_EMPLOYER", "EMPLOYER_CONTRIBUTION");
        Long pensionEmployeeComponentId = requireSystemComponent(
                "NSSF_PENSION_EMPLOYEE", "DEDUCTION");
        Long pensionEmployerComponentId = requireSystemComponent(
                "NSSF_PENSION_EMPLOYER", "EMPLOYER_CONTRIBUTION");

        calculationRepository.insertCalculatedItems(
                runId,
                payrollEmployeeId,
                healthEmployeeComponentId,
                healthEmployerComponentId,
                riskEmployeeComponentId,
                riskEmployerComponentId,
                pensionEmployeeComponentId,
                pensionEmployerComponentId,
                userId);
    }

    private Long requireSystemComponent(String code, String type) {
        PayrollComponent component = componentRepository.findByComponentCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Active payroll component " + code + " (" + type + ") is required."));
        if (!component.isActive() || !type.equalsIgnoreCase(component.getComponentType())) {
            throw new IllegalStateException(
                    "Active payroll component " + code + " (" + type + ") is required.");
        }
        return component.getId();
    }

    private void validateNssfBandConfiguration(Long configId) {
        List<PayrollNssfWageBand> bands = wageBandRepository
                .findByPayrollNssfConfigIdOrderByBandOrderAsc(configId);
        if (bands.isEmpty()) {
            throw new IllegalStateException(
                    "NSSF Rules require wage bands. | ច្បាប់ ប.ស.ស. ត្រូវមានកម្រិតប្រាក់ឈ្នួល។");
        }

        BigDecimal previousMaximum = null;
        BigDecimal previousWage = null;
        for (int index = 0; index < bands.size(); index++) {
            PayrollNssfWageBand band = bands.get(index);
            int expectedOrder = index + 1;
            if (!Integer.valueOf(expectedOrder).equals(band.getBandOrder())) {
                throw new IllegalStateException(
                        "NSSF band order must be continuous from 1. Missing order " + expectedOrder
                                + ". | លំដាប់កម្រិត ប.ស.ស. ត្រូវបន្តគ្នាចាប់ពីលេខ 1។");
            }

            boolean finalBand = index == bands.size() - 1;
            if (band.getMaxSalary() == null && !finalBand) {
                throw new IllegalStateException(
                        "Only the final NSSF band may have no maximum. "
                                + "| មានតែកម្រិត ប.ស.ស. ចុងក្រោយប៉ុណ្ណោះដែលអាចគ្មានអតិបរមា។");
            }
            if (finalBand && band.getMaxSalary() != null) {
                throw new IllegalStateException(
                        "The final NSSF band must have no maximum. "
                                + "| កម្រិត ប.ស.ស. ចុងក្រោយត្រូវគ្មានប្រាក់បៀវត្សអតិបរមា។");
            }
            if (previousMaximum != null
                    && band.getMaxSalary() != null
                    && band.getMaxSalary().compareTo(previousMaximum) <= 0) {
                throw new IllegalStateException(
                        "NSSF maximum salaries must increase with each band. "
                                + "| ប្រាក់បៀវត្សអតិបរមា ប.ស.ស. ត្រូវកើនតាមលំដាប់កម្រិត។");
            }
            if (previousWage != null
                    && band.getContributoryWage().compareTo(previousWage) < 0) {
                throw new IllegalStateException(
                        "NSSF contributory wages cannot decrease in higher bands. "
                                + "| ប្រាក់ឈ្នួលជាប់ភាគទាន ប.ស.ស. មិនអាចថយចុះនៅកម្រិតខ្ពស់ជាងបានទេ។");
            }

            previousMaximum = band.getMaxSalary();
            previousWage = band.getContributoryWage();
        }
    }
}

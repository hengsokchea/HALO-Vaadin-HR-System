package org.halocambodia.services;

import org.halocambodia.data.*;
import org.halocambodia.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentEmployeeService {

    private final AuthenticatedUser authenticatedUser;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public User requireUser() {
        return authenticatedUser.get()
                .orElseThrow(() -> new IllegalStateException("User is not logged in"));
    }

    @Transactional(readOnly = true)
    public Employee requireEmployee() {
        User user = requireUser();
        String insurance = user.getInsurance();
        if (insurance == null || insurance.isBlank()) {
            throw new IllegalStateException("The logged-in user is not linked to an employee insurance number");
        }
        try {
            return employeeRepository.findByInsuranceNo(Integer.valueOf(insurance.trim()))
                    .orElseThrow(() -> new IllegalStateException(
                            "No employee was found for insurance number " + insurance));
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid insurance number on the user account: " + insurance, ex);
        }
    }
}

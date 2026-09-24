package org.halocambodia.services;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAllocation;
import org.halocambodia.data.EmployeeAllocationRepository;
import org.halocambodia.data.ShiftCycle;
import org.halocambodia.data.ShiftCycleRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.employee_allocate.EmployeeAllocationCloneView;
import org.halocambodia.views.employee_allocate.EmployeeAllocationView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAllocationCloneService {

    private final EmployeeAllocationRepository repository;
    private final ShiftCycleRepository shiftCycleRepository;
    private final AuthenticatedUser authenticatedUser;

    public EmployeeAllocationCloneService(EmployeeAllocationRepository repository,
                                          ShiftCycleRepository shiftCycleRepository,
                                          AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.shiftCycleRepository = shiftCycleRepository;
        this.authenticatedUser = authenticatedUser;
    }

    /**
     * Clone allocations from one period to another using explicitly provided user.
     * Safe for background thread execution.
     */
    @Transactional
    public List<EmployeeAllocation> cloneAllocationsForUser(
            int fromMonth, int fromYear, int toMonth, int toYear, User currentUser) {

        if (currentUser == null)
            throw new IllegalArgumentException("User cannot be null");

        if (!authenticatedUser.hasPage(EmployeeAllocationCloneView.class, AccessPageType.SELECTED_PAGE)) {
            throw new IllegalArgumentException("You don't have permission to clone allocations.");
        }

        // Load source records
        List<EmployeeAllocation> sourceAllocations =
                repository.findByMonthNumAndYearNum(fromMonth, fromYear);

        if (sourceAllocations.isEmpty()) {
            return List.of();
        }

        return sourceAllocations.stream()
                .map(src -> {
                    // Skip if already exists for same employee/month/year
                    if (repository.existsByEmployeeAndYearNumAndMonthNum(
                            src.getEmployee(), toYear, toMonth)) {
                        return null;
                    }

                    EmployeeAllocation clone = new EmployeeAllocation();

                    clone.setId(null);
                    clone.setEmployee(src.getEmployee());
                    clone.setYearNum(toYear);
                    clone.setMonthNum(toMonth);
                    clone.setPositions(src.getPositions());
                    clone.setTeams(src.getTeams());
                    clone.setBranch(src.getBranch());
                    clone.setCategory(src.getCategory());
                    clone.setContracts(src.getContracts());
                    clone.setPoGrade(src.getPoGrade());
                    clone.setShiftCycle(src.getShiftCycle());
                    
                    clone.setUserCreated(currentUser);
                    clone.setUserUpdated(currentUser);
                    clone.setCreatedAt(ZonedDateTime.now());
                    clone.setUpdatedAt(ZonedDateTime.now());
                    return clone;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

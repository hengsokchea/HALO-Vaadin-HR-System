package org.halocambodia.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Employee;
import org.halocambodia.data.EmployeeAllocation;
import org.halocambodia.data.EmployeeAllocationRepository;
import org.halocambodia.data.EmployeeRosterRepository;
import org.halocambodia.data.Holiday;
import org.halocambodia.data.HolidayRepository;
import org.halocambodia.data.Shift;
import org.halocambodia.data.ShiftCycle;
import org.halocambodia.data.ShiftCycleDetail;
import org.halocambodia.data.ShiftCycleDetailRepository;
import org.halocambodia.data.ShiftCycleRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.employee_allocate.EmployeeAllocationView;
import org.halocambodia.views.roster.ShiftView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class EmployeeAllocationService implements GenericService<EmployeeAllocation> {

    private final EmployeeAllocationRepository repository;
    private final AuthenticatedUser authenticatedUser;
    private final EmployeeRosterRepository rosterRepository;

    public EmployeeAllocationService(EmployeeAllocationRepository repository,AuthenticatedUser authenticatedUser,EmployeeRosterRepository rosterRepository ) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
        this.rosterRepository = rosterRepository;

    }

    @Override
    public Page<EmployeeAllocation> list(Pageable pageable, Specification<EmployeeAllocation> filter) {
        return repository.findAll(filter, pageable);
    }

    @Override
    public long count(Specification<EmployeeAllocation> filter) {
        Specification<EmployeeAllocation> spec = (filter != null) ? filter : null;
        return repository.count(spec);
    }

    @Override
    @Transactional
    public void delete(Set<EmployeeAllocation> entities) {
        if (!authenticatedUser.hasPage(EmployeeAllocationView.class, AccessPageType.DELETED_PAGE)) {
            throw new IllegalArgumentException("You don't have permission to do this operation ");
        }
        if (entities == null || entities.isEmpty()) return;

        var ids = entities.stream()
                .map(EmployeeAllocation::getId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        if (!ids.isEmpty()) {
            rosterRepository.deleteByEmployeeAllocationIds(ids);
        }

        repository.deleteAllInBatch(entities);
    }

    @Override
    @Transactional
    public EmployeeAllocation update(EmployeeAllocation entityValue) {
        User currentUserLogin = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

        // audit
        entityValue.setUserCreated(currentUserLogin); // if new, otherwise ignored by your entity
        entityValue.setUserUpdated(currentUserLogin);

        boolean isUpdating = entityValue.getId() != null;

        AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(EmployeeAllocationView.class, accessPageType)) {
            throw new IllegalArgumentException("You don't have permission to perform this operation");
        }
        return repository.save(entityValue);
        
    }
    
    @Transactional
    public List<EmployeeAllocation> updateAll(List <EmployeeAllocation> entityValues) {
        User currentUserLogin = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

        // audit
        //entityValues.setUserCreated(currentUserLogin); // if new, otherwise ignored by your entity
        //entityValues.setUserUpdated(currentUserLogin);
        
        //entityValues.forEach(aVs -> {
        //	aVs.setUserCreated(currentUserLogin);
        //	aVs.setUserUpdated(currentUserLogin);
        //});


        boolean isUpdating = false;

        AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(EmployeeAllocationView.class, accessPageType)) {
            throw new IllegalArgumentException("You don't have permission to perform this operation");
        }
        return repository.saveAll(entityValues);
        
    }
    

    public List<EmployeeAllocation> findAll(Specification<EmployeeAllocation> filter) {
        return repository.findAll((filter != null) ? filter : null);
    }


    public boolean existsByEmployeeAndYearAndMonth(Employee employee, Integer year, Integer month) {
        return repository.existsByEmployeeAndYearNumAndMonthNum(employee, year, month);
    }

    public void updateExistingAllocations(List<EmployeeAllocation> allocations) {
        for (EmployeeAllocation allocation : allocations) {
            // Find existing allocation
            Optional<EmployeeAllocation> existingOpt = repository.findByEmployeeAndYearNumAndMonthNum(
                allocation.getEmployee(), 
                allocation.getYearNum(), 
                allocation.getMonthNum()
            );
            
            if (existingOpt.isPresent()) {
                EmployeeAllocation existing = existingOpt.get();
                
                // Update fields from the new allocation
                existing.setTeams(allocation.getTeams());
                existing.setPositions(allocation.getPositions());
                existing.setBranch(allocation.getBranch());
                existing.setCategory(allocation.getCategory());
                existing.setPoGrade(allocation.getPoGrade());
                existing.setContracts(allocation.getContracts());
                existing.setShiftCycle(allocation.getShiftCycle());
                existing.setUserUpdated(allocation.getUserUpdated());
                existing.setUpdatedAt(ZonedDateTime.now());
                
                repository.save(existing);
            }
        }
    }

    public void saveAll(List<EmployeeAllocation> allocations) {
        repository.saveAll(allocations);
    }

}

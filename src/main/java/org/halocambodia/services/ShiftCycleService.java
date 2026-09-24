package org.halocambodia.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Holiday;
import org.halocambodia.data.HolidayRepository;
import org.halocambodia.data.Shift;
import org.halocambodia.data.ShiftCycle;
import org.halocambodia.data.ShiftCycleDetail;
import org.halocambodia.data.ShiftCycleDetailRepository;
import org.halocambodia.data.ShiftCycleRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.roster.ShiftView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class ShiftCycleService implements GenericService<ShiftCycle> {

    private final ShiftCycleRepository repository;
    private final AuthenticatedUser authenticatedUser;
    private final ShiftCycleDetailRepository shiftCycleDetailRepository;
    private final HolidayRepository holidayRepository;

    public ShiftCycleService(
            ShiftCycleRepository repository,
            AuthenticatedUser authenticatedUser,
            ShiftCycleDetailRepository shiftCycleDetailRepository,
            HolidayRepository holidayRepository
    ) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
        this.shiftCycleDetailRepository = shiftCycleDetailRepository;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public Page<ShiftCycle> list(Pageable pageable, Specification<ShiftCycle> filter) {
        return repository.findAll(filter, pageable);
    }

    @Override
    public long count(Specification<ShiftCycle> filter) {
        Specification<ShiftCycle> spec = (filter != null) ? filter : null;
        return repository.count(spec);
    }

    @Override
    public void delete(Set<ShiftCycle> entity) {
        if (!authenticatedUser.hasPage(ShiftView.class, AccessPageType.DELETED_PAGE)) {
            throw new IllegalArgumentException("You don't have permission to do this operation ");
        }
        repository.deleteAllInBatch(entity);
    }

    @Override
    @Transactional
    public ShiftCycle update(ShiftCycle entityValue) {
        User currentUserLogin = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

        // audit
        entityValue.setUserCreated(currentUserLogin); // if new, otherwise ignored by your entity
        entityValue.setUserUpdated(currentUserLogin);

        boolean isUpdating = entityValue.getId() != null;

        AccessPageType accessPageType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(ShiftView.class, accessPageType)) {
            throw new IllegalArgumentException("You don't have permission to perform this operation");
        }
        return repository.save(entityValue);
    }

    public List<ShiftCycle> findAll(Specification<ShiftCycle> filter) {
        return repository.findAll((filter != null) ? filter : null);
    }

    @Transactional
    public int generateShiftCycleRaw(int shiftId, int yearNumber, boolean includeWeekends) {
        User currentUser = authenticatedUser.get()
            .orElseThrow(() -> new IllegalArgumentException("User not logged in"));
        return repository.generateShiftCycleRaw(shiftId, yearNumber, currentUser.getId(), includeWeekends);
    }

    
    public Optional<ShiftCycle> findCycle(Integer shiftId, Integer year) {
        return repository.findByShift_IdAndYear(shiftId, year);
    }

 // Modify the method signature
    @Transactional
    public void updateHolidayForDetailId(Long shiftCycleDetailId, Holiday holiday) {
        if (shiftCycleDetailId == null || holiday == null) {
            throw new IllegalArgumentException("ShiftCycleDetail ID and Holiday must not be null.");
        }
        
        shiftCycleDetailRepository.findById(shiftCycleDetailId).ifPresent(detail -> {
            // Update the holiday
            detail.setHoliday(holiday);
            // Save the updated detail
            shiftCycleDetailRepository.save(detail);
        });
    }

}

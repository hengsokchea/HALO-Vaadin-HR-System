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
import org.halocambodia.data.ShiftRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.roster.ShiftView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class ShiftService implements GenericService<Shift> {

    private final ShiftRepository repository;
    private final AuthenticatedUser authenticatedUser;


    public ShiftService(
    		ShiftRepository repository,
            AuthenticatedUser authenticatedUser
    ) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public Page<Shift> list(Pageable pageable, Specification<Shift> filter) {
        return filter == null
                ? repository.findAll(pageable)
                : repository.findAll(filter, pageable);
    }

    @Override
    public long count(Specification<Shift> filter) {
        return filter == null
                ? repository.count()
                : repository.count(filter);
    }

    @Override
    public void delete(Set<Shift> entity) {
        if (!authenticatedUser.hasPage(ShiftView.class, AccessPageType.DELETED_PAGE)) {
            throw new IllegalArgumentException("You don't have permission to do this operation ");
        }
        repository.deleteAllInBatch(entity);
    }

    @Override
    @Transactional
    public Shift update(Shift entityValue) {
        User currentUser = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in"));

        // === Duplicate Shift Name Check (case-insensitive) ===
        Optional<Shift> existing = repository.findByShiftNameIgnoreCase(entityValue.getShiftName());
        if (existing.isPresent() && (entityValue.getId() == null || !existing.get().getId().equals(entityValue.getId()))) {
            throw new IllegalArgumentException("Shift name already exists");
        }

        // === Permission Check ===
        boolean isUpdating = entityValue.getId() != null;
        AccessPageType accessType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(ShiftView.class, accessType)) {
            throw new IllegalArgumentException("You don't have permission to perform this operation.");
        }

        // === Audit Fields ===
        entityValue.setUserUpdated(currentUser);
        if (!isUpdating) {
            entityValue.setUserCreated(currentUser);
        }

        return repository.save(entityValue);
    }

    public List<Shift> findAll(Specification<Shift> filter) {
        return filter == null
                ? repository.findAll()
                : repository.findAll(filter);
    }


}

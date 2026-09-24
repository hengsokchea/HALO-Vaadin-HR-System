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
import org.halocambodia.views.roster.HolidayView;
import org.halocambodia.views.roster.ShiftView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class HolidayService implements GenericService<Holiday> {

    private final HolidayRepository repository;
    private final AuthenticatedUser authenticatedUser;


    public HolidayService(
    		HolidayRepository repository,
            AuthenticatedUser authenticatedUser
    ) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public Page<Holiday> list(Pageable pageable, Specification<Holiday> filter) {
        return repository.findAll(filter, pageable);
    }

    @Override
    public long count(Specification<Holiday> filter) {
        Specification<Holiday> spec = (filter != null) ? filter : null;
        return repository.count(spec);
    }

    @Override
    public void delete(Set<Holiday> entity) {
        if (!authenticatedUser.hasPage(HolidayView.class, AccessPageType.DELETED_PAGE)) {
            throw new IllegalArgumentException("You don't have permission to do this operation ");
        }
        repository.deleteAllInBatch(entity);
    }

    @Override
    @Transactional
    public Holiday update(Holiday entityValue) {
        User currentUser = authenticatedUser.get()
                .orElseThrow(() -> new IllegalArgumentException("User not logged in"));



        // === Permission Check ===
        boolean isUpdating = entityValue.getId() != null;
        AccessPageType accessType = isUpdating ? AccessPageType.UPDATED_PAGE : AccessPageType.INSERTED_PAGE;
        if (!authenticatedUser.hasPage(HolidayView.class, accessType)) {
            throw new IllegalArgumentException("You don't have permission to perform this operation.");
        }

        // === Audit Fields ===
        entityValue.setUserUpdated(currentUser);
        if (!isUpdating) {
            entityValue.setUserCreated(currentUser);
        }

        return repository.save(entityValue);
    }

    public List<Holiday> findAll(Specification<Holiday> filter) {
        return repository.findAll((filter != null) ? filter : null);
    }


}

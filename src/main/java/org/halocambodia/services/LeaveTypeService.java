package org.halocambodia.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.LeaveType;
import org.halocambodia.data.LeaveTypeRepository;
import org.halocambodia.data.LeaveTypeSubType;
import org.halocambodia.data.User;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.attendance_management.DailyAttendanceView;
import org.halocambodia.views.leave_management.LeaveTypeView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LeaveTypeService implements GenericService<LeaveType> {

    private final LeaveTypeRepository repository;
    private final AuthenticatedUser authenticatedUser;

    public LeaveTypeService(LeaveTypeRepository repository, AuthenticatedUser authenticatedUser) {
        this.repository = repository;
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveType> list(Pageable pageable, Specification<LeaveType> specification) {
        return repository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<LeaveType> specification) {
        return specification == null ? repository.count() : repository.count(specification);
    }

    @Override
    public LeaveType update(LeaveType entity) {
        return updateWithSubTypes(entity, entity == null ? List.of() : entity.getLeaveTypeSubTypes());
    }

    public LeaveType updateWithSubTypes(LeaveType input, List<LeaveTypeSubType> subTypes) {
        if (input == null) {
            throw new IllegalArgumentException("Duty / Leave is required. | ត្រូវការកាតព្វកិច្ច / ច្បាប់ឈប់សម្រាក។");
        }
        boolean isNew = input.getId() == null;
        checkPermission(isNew ? AccessPageType.INSERTED_PAGE : AccessPageType.UPDATED_PAGE);
        User user = authenticatedUser.get().orElseThrow(() ->
                new IllegalArgumentException("User is not logged in. | អ្នកប្រើប្រាស់មិនទាន់ចូលប្រព័ន្ធ។"));

        LeaveType target = isNew ? new LeaveType() : repository.findOneById(input.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Duty / Leave no longer exists. Refresh the page. | ទិន្នន័យកាតព្វកិច្ច / ច្បាប់នេះលែងមាន។ សូមផ្ទុកទំព័រឡើងវិញ។"));
        if (isNew) target.setUserCreated(user);
        target.setUserUpdated(user);
        target.setLeavTypeCode(input.getLeavTypeCode());
        target.setLeaveNameEn(input.getLeaveNameEn());
        target.setLeaveNameKh(input.getLeaveNameKh());
        target.setLeaveTypeGroup(input.getLeaveTypeGroup());
        target.setLeaveRate(input.getLeaveRate());
        target.setEntitledDays(input.getEntitledDays());
        target.setPaid(input.getPaid());
        target.setObsoleteDate(input.getObsoleteDate());
        target.setRemarks(input.getRemarks());

        Map<Long, LeaveTypeSubType> existingById = target.getLeaveTypeSubTypes().stream()
                .filter(row -> row.getId() != null)
                .collect(Collectors.toMap(LeaveTypeSubType::getId, Function.identity()));
        List<LeaveTypeSubType> reconciled = new java.util.ArrayList<>();
        if (subTypes != null) {
            for (LeaveTypeSubType row : subTypes) {
                LeaveTypeSubType child = row.getId() == null
                        ? new LeaveTypeSubType()
                        : Optional.ofNullable(existingById.remove(row.getId()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "A Sub Duty / Leave changed or was deleted. Refresh and try again. "
                                                + "| ប្រភេទរងបានផ្លាស់ប្តូរ។ សូមផ្ទុកទំព័រឡើងវិញ។"));
                child.setLeaveSubTypeNameEn(row.getLeaveSubTypeNameEn());
                child.setLeaveSubTypeNameKh(row.getLeaveSubTypeNameKh());
                child.setEntitledDay(row.getEntitledDay());
                child.setObsoleteDate(row.getObsoleteDate());
                child.setRemarks(row.getRemarks());
                child.setLeaveType(target);
                if (child.getId() == null) child.setUserCreated(user);
                child.setUserUpdated(user);
                reconciled.add(child);
            }
        }
        target.getLeaveTypeSubTypes().removeIf(row -> !reconciled.contains(row));
        for (LeaveTypeSubType child : reconciled) {
            if (!target.getLeaveTypeSubTypes().contains(child)) target.getLeaveTypeSubTypes().add(child);
        }
        LeaveType saved = repository.saveAndFlush(target);
        return repository.findOneById(saved.getId()).orElse(saved);
    }

    @Override
    public void delete(Set<LeaveType> entities) {
        if (entities == null || entities.isEmpty()) return;
        checkPermission(AccessPageType.DELETED_PAGE);
        repository.deleteAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveType> findAll(Specification<LeaveType> specification) {
        return specification == null ? repository.findAll() : repository.findAll(specification);
    }

    @Transactional(readOnly = true)
    public Optional<LeaveType> findById(Long id) {
        return id == null ? Optional.empty() : repository.findOneById(id);
    }

    private void checkPermission(AccessPageType action) {
        if (!authenticatedUser.hasPage(LeaveTypeView.class, action)
                && !authenticatedUser.hasPage(DailyAttendanceView.class, action)) {
            throw new IllegalArgumentException("You do not have permission for this operation. | អ្នកមិនមានសិទ្ធិសម្រាប់ប្រតិបត្តិការនេះទេ។");
        }
    }
}

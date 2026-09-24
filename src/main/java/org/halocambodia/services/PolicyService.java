package org.halocambodia.services;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.halocambodia.data.AccessPageType;
import org.halocambodia.data.Policy;
import org.halocambodia.data.PolicyCategory;
import org.halocambodia.data.PolicyCategoryRepository;
import org.halocambodia.data.PolicyRepository;
import org.halocambodia.data.User;
import org.halocambodia.security.*;
import org.halocambodia.security.AuthenticatedUser;
import org.halocambodia.views.policy.PolicyView;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PolicyService        implements GenericService<Policy> {

    private static final int MAXIMUM_CATEGORY_SEQUENCE = 999;

    private final PolicyRepository repository;
    private final PolicyCategoryRepository categoryRepository;
    private final AuthenticatedUser authenticatedUser;

    public PolicyService( PolicyRepository repository,  PolicyCategoryRepository categoryRepository, AuthenticatedUser authenticatedUser) {

        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Policy> list(
            Pageable pageable,
            Specification<Policy> filter) {

        return repository.findAll(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(Specification<Policy> filter) {
        return filter == null
                ? repository.count()
                : repository.count(filter);
    }

    @Override
    public void delete(Set<Policy> entities) {
        if (!authenticatedUser.hasPage(
                PolicyView.class,
                AccessPageType.DELETED_PAGE)) {

            throw new IllegalArgumentException(
                    "You don't have permission to delete policies."
            );
        }

        repository.deleteAll(entities);
    }

    /*
     * Used by PolicyView to display a preview immediately
     * after the category is selected.
     *
     * The final code is generated again with a database lock
     * during save.
     */
    @Transactional(readOnly = true)
    public String previewNextCode(
            PolicyCategory category) {

        if (category == null) {
            return "";
        }

        return buildNextCode(category.getCode());
    }

    /*
     * Generate the final code while holding a database lock
     * on the selected policy category.
     */
    private String generateCodeForCategory(
            PolicyCategory selectedCategory) {

        if (selectedCategory == null
                || selectedCategory.getId() == null) {

            throw new IllegalArgumentException(
                    "Policy category is required."
            );
        }

        PolicyCategory lockedCategory =
                categoryRepository
                    .findByIdForUpdate(
                        selectedCategory.getId()
                    )
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "The selected policy category no longer exists."
                        )
                    );

        return buildNextCode(lockedCategory.getCode());
    }

    private String buildNextCode(String categoryCode) {
        String prefix = normalizeCategoryCode(categoryCode);

        Integer maximum =
                repository.findMaximumCodeNumber(prefix);

        int nextNumber =
                (maximum == null ? 0 : maximum) + 1;

        if (nextNumber > MAXIMUM_CATEGORY_SEQUENCE) {
            throw new IllegalStateException(
                    "Policy category "
                        + prefix
                        + " has reached its maximum code number "
                        + MAXIMUM_CATEGORY_SEQUENCE
                        + "."
            );
        }

        return prefix
                + String.format(
                    Locale.ROOT,
                    "%03d",
                    nextNumber
                );
    }

    private String normalizeCategoryCode(
            String categoryCode) {

        if (categoryCode == null
                || categoryCode.isBlank()) {

            throw new IllegalArgumentException(
                    "Policy category code is required."
            );
        }

        String normalized =
                categoryCode
                    .trim()
                    .toUpperCase(Locale.ROOT);

        if (normalized.length() + 3 > 100) {
            throw new IllegalArgumentException(
                    "Policy category code is too long."
            );
        }

        return normalized;
    }

    @Override
    public Policy update(Policy entityValue) {
        User currentUser =
                authenticatedUser.get()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            "User not logged in."
                        )
                    );

        boolean isUpdating =
                entityValue.getId() != null;

        AccessPageType requiredPermission =
                isUpdating
                    ? AccessPageType.UPDATED_PAGE
                    : AccessPageType.INSERTED_PAGE;

        if (!authenticatedUser.hasPage(
                PolicyView.class,
                requiredPermission)) {

            throw new IllegalArgumentException(
                "You don't have permission to perform this operation."
            );
        }

        if (isUpdating) {
            Policy existing =
                    repository.findById(entityValue.getId())
                        .orElseThrow(() ->
                            new IllegalArgumentException(
                                "This policy no longer exists."
                            )
                        );

            Long existingCategoryId =
                    existing.getPolicyCategory() != null
                        ? existing
                            .getPolicyCategory()
                            .getId()
                        : null;

            Long selectedCategoryId =
                    entityValue.getPolicyCategory() != null
                        ? entityValue
                            .getPolicyCategory()
                            .getId()
                        : null;

            boolean categoryChanged =
                    !Objects.equals(
                        existingCategoryId,
                        selectedCategoryId
                    );

            if (categoryChanged) {
                /*
                 * Generate the next code for the new category.
                 */
                entityValue.setCode(
                    generateCodeForCategory(
                        entityValue.getPolicyCategory()
                    )
                );
            } else {
                /*
                 * Preserve the existing code when the category
                 * has not changed.
                 */
                entityValue.setCode(
                    existing.getCode()
                );
            }
        } else {
            entityValue.setCode(
                generateCodeForCategory(
                    entityValue.getPolicyCategory()
                )
            );

            entityValue.setUserCreated(currentUser);
        }

        entityValue.setUserUpdated(currentUser);

        return repository.save(entityValue);
    }

    @Transactional(readOnly = true)
    public List<Policy> findAll(
            Specification<Policy> filter) {

        return filter == null
                ? repository.findAll()
                : repository.findAll(filter);
    }
}
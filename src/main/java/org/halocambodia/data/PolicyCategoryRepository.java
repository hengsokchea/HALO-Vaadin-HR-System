package org.halocambodia.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PolicyCategoryRepository        extends JpaRepository<PolicyCategory, Long>,                JpaSpecificationExecutor<PolicyCategory> {

    List<PolicyCategory> findByObsoleteDateIsNull();

    Optional<PolicyCategory> findByCode(String code);

    List<PolicyCategory>
            findByObsoleteDateIsNullOrderByNameEnAsc();

    /*
     * Lock the selected category while generating and saving
     * the next policy code.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT category
        FROM PolicyCategory category
        WHERE category.id = :id
        """)
    Optional<PolicyCategory> findByIdForUpdate(
            @Param("id") Long id);
}